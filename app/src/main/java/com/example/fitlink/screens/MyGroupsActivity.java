package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.textfield.TextInputLayout;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MyGroupsActivity extends BaseActivity {

    private static final String TAG = "MyGroupsActivity";

    private GroupAdapter adapter;
    private TextView tvGroupCount;
    private EditText etSearch;
    private Spinner spinnerSearchType, spinnerSearchOptions;
    private TextInputLayout layoutSearchText;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private RecyclerView rvMyGroups;

    private List<Group> allMyGroups = new ArrayList<>();

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
        etSearch = findViewById(R.id.edit_GroupsList_search);
        spinnerSearchType = findViewById(R.id.spinner_groups_search_type);
        spinnerSearchOptions = findViewById(R.id.spinner_search_options);
        layoutSearchText = findViewById(R.id.layout_search_text);
        progressBar = findViewById(R.id.my_groups_progress_bar);
        emptyState = findViewById(R.id.my_groups_empty_state);
        rvMyGroups = findViewById(R.id.rv_my_groups_list);
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
        String[] searchOptions = {"Name", "Sport Type", "Level", "Location"};
        spinnerSearchType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, searchOptions));

        DifficultyLevel[] levels = DifficultyLevel.values();
        String[] levelNames = new String[levels.length];
        for(int i=0; i<levels.length; i++) levelNames[i] = levels[i].getDisplayName();
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, levelNames);

        SportType[] sports = SportType.values();
        String[] sportNames = new String[sports.length];
        for(int i=0; i<sports.length; i++) sportNames[i] = sports[i].getDisplayName();
        ArrayAdapter<String> sportAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sportNames);

        spinnerSearchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = searchOptions[position];

                if (selectedType.equals("Level")) {
                    layoutSearchText.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setAdapter(levelAdapter);
                } else if (selectedType.equals("Sport Type")) {
                    layoutSearchText.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setAdapter(sportAdapter);
                } else {
                    layoutSearchText.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setVisibility(View.GONE);
                }
                executeSearch();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSearchOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { executeSearch(); }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { executeSearch(); }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        rvMyGroups.setLayoutManager(new LinearLayoutManager(this));

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        adapter = new GroupAdapter(new ArrayList<>(), false, currentUserId, new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onJoinClick(Group group) {}

            @Override
            public void onLeaveClick(Group group) {}

            @Override
            public void onGroupClick(Group group) {
                Intent intent = new Intent(MyGroupsActivity.this, GroupDashboardActivity.class);
                intent.putExtra("GROUP_EXTRA", group);
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
        String searchType = spinnerSearchType.getSelectedItem() != null ? spinnerSearchType.getSelectedItem().toString() : "Name";
        List<Group> filteredList;

        if (searchType.equals("Level")) {
            if (spinnerSearchOptions.getSelectedItem() == null) return;
            String selectedLvl = spinnerSearchOptions.getSelectedItem().toString();
            filteredList = allMyGroups.stream()
                    .filter(g -> g.getLevel() != null && g.getLevel().getDisplayName().equals(selectedLvl))
                    .collect(Collectors.toList());
        } else if (searchType.equals("Sport Type")) {
            if (spinnerSearchOptions.getSelectedItem() == null) return;
            String selectedSport = spinnerSearchOptions.getSelectedItem().toString();
            filteredList = allMyGroups.stream()
                    .filter(g -> g.getSportType() != null && g.getSportType().getDisplayName().equals(selectedSport))
                    .collect(Collectors.toList());
        } else {
            String query = etSearch.getText().toString().toLowerCase().trim();
            if (query.isEmpty()) {
                updateListDisplay(allMyGroups);
                return;
            }
            filteredList = allMyGroups.stream().filter(group -> {
                if (searchType.equals("Name")) return group.getName() != null && group.getName().toLowerCase().contains(query);
                if (searchType.equals("Location")) return group.getLocation() != null && group.getLocation().getAddress() != null && group.getLocation().getAddress().toLowerCase().contains(query);
                return false;
            }).collect(Collectors.toList());
        }
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