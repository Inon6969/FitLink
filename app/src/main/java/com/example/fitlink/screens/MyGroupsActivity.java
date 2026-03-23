package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.adapters.GroupAdapter;
import com.example.fitlink.models.DifficultyLevel;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.GroupFilterDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MyGroupsActivity extends BaseActivity {

    private GroupAdapter adapter;
    private TextView tvGroupCount;
    private EditText etSearch;
    private MaterialButton btnFilter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private RecyclerView rvMyGroups;

    private List<Group> allMyGroups = null;

    // משתנים לשמירת המצב הנוכחי של הסינון
    private SportType activeSportFilter = null;
    private DifficultyLevel activeLevelFilter = null;
    private String activeLocationFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_groups);

        initViews();
        setupToolbar();
        setupSearchLogic();
        setupRecyclerView();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_my_groups_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvGroupCount = findViewById(R.id.tv_my_group_count);
        etSearch = findViewById(R.id.edit_MyGroups_search);
        btnFilter = findViewById(R.id.btn_filter_my_groups);
        progressBar = findViewById(R.id.my_groups_progress_bar);
        emptyState = findViewById(R.id.my_groups_empty_state);
        rvMyGroups = findViewById(R.id.rv_my_groups_list);

        MaterialButton btnExploreGroups = findViewById(R.id.btn_explore_groups);
        btnExploreGroups.setOnClickListener(v -> {
            Intent intent = new Intent(MyGroupsActivity.this, GroupsListActivity.class);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_my_groups);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupSearchLogic() {
        // חיפוש טקסטואלי רגיל (לפי שם)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { executeSearch(); }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // פתיחת דיאלוג הסינון בדיוק כמו ב-GroupsListActivity
        btnFilter.setOnClickListener(v -> {
            GroupFilterDialog dialog = new GroupFilterDialog(this, activeSportFilter, activeLevelFilter, activeLocationFilter,
                    (sportType, level, location) -> {
                        // שומרים את בחירות המשתמש
                        activeSportFilter = sportType;
                        activeLevelFilter = level;
                        activeLocationFilter = (location != null) ? location : "";

                        // מריצים סינון מחדש
                        executeSearch();
                    });
            dialog.show();
        });
    }

    private void setupRecyclerView() {
        rvMyGroups.setLayoutManager(new LinearLayoutManager(this));

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        adapter = new GroupAdapter(new ArrayList<>(), false, currentUserId, new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onJoinClick(Group group) {} // לא רלוונטי במסך זה

            @Override
            public void onLeaveClick(Group group) {} // לא רלוונטי במסך זה

            @Override
            public void onGroupClick(Group group) {
                Intent intent = new Intent(MyGroupsActivity.this, GroupDashboardActivity.class);
                intent.putExtra("GROUP_ID", group.getId());
                startActivity(intent);
            }
        });

        rvMyGroups.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyGroups();
    }

    private void loadMyGroups() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        databaseService.getUser(Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(User user) {
                if (user == null || user.getGroupIds() == null || user.getGroupIds().isEmpty()) {
                    allMyGroups = new ArrayList<>();
                    executeSearch();
                    return;
                }

                Map<String, Boolean> myGroupIds = user.getGroupIds();

                databaseService.getAllGroups(new DatabaseService.DatabaseCallback<>() {
                    @Override
                    public void onCompleted(List<Group> allGroupsList) {
                        allMyGroups = new ArrayList<>();
                        for (Group group : allGroupsList) {
                            if (myGroupIds.containsKey(group.getId())) {
                                allMyGroups.add(group);
                            }
                        }
                        executeSearch();
                    }

                    @Override
                    public void onFailed(Exception e) { handleError(e); }
                });
            }
            @Override
            public void onFailed(Exception e) { handleError(e); }
        });
    }

    private void executeSearch() {
        if (allMyGroups == null) return;

        String nameQuery = etSearch.getText().toString().toLowerCase().trim();

        List<Group> filteredList = allMyGroups.stream().filter(group -> {
            // 1. סינון לפי שם
            boolean matchesName = nameQuery.isEmpty() ||
                    (group.getName() != null && group.getName().toLowerCase().contains(nameQuery));

            // 2. סינון לפי ספורט
            boolean matchesSport = activeSportFilter == null ||
                    (group.getSportType() != null && group.getSportType() == activeSportFilter);

            // 3. סינון לפי רמה
            boolean matchesLevel = activeLevelFilter == null ||
                    (group.getLevel() != null && group.getLevel() == activeLevelFilter);

            // 4. סינון לפי מיקום
            boolean matchesLocation = activeLocationFilter.isEmpty() ||
                    (group.getLocation() != null && group.getLocation().getAddress() != null &&
                            group.getLocation().getAddress().toLowerCase().contains(activeLocationFilter.toLowerCase()));

            return matchesName && matchesSport && matchesLevel && matchesLocation;
        }).collect(Collectors.toList());

        updateListDisplay(filteredList);
    }

    private void updateListDisplay(List<Group> listToDisplay) {
        progressBar.setVisibility(View.GONE);
        if (adapter != null) adapter.updateList(listToDisplay);
        tvGroupCount.setText(MessageFormat.format("Showing {0} groups", listToDisplay.size()));
        emptyState.setVisibility(listToDisplay.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void handleError(Exception e) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(MyGroupsActivity.this, "Error loading groups", Toast.LENGTH_SHORT).show();
    }
}