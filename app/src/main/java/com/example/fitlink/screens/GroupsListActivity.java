package com.example.fitlink.screens;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.fitlink.models.Group;
import com.example.fitlink.screens.dialogs.AddGroupDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GroupsListActivity extends BaseActivity {

    private static final String TAG = "GroupsListActivity";

    // UI Elements
    private GroupAdapter groupAdapter;
    private TextView tvGroupCount;
    private EditText etSearch;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnCreateGroup;

    // Data
    private List<Group> allGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_groups_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchLogic();
        setupCreateGroupButton();
    }

    private void initViews() {
        // התאמת Insets למסך מלא (Full Screen) בדומה ל-UsersListActivity
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_groups_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvGroupCount = findViewById(R.id.tv_group_count);
        etSearch = findViewById(R.id.edit_GroupsList_search);
        progressBar = findViewById(R.id.groups_progress_bar);
        emptyState = findViewById(R.id.groups_empty_state);
        btnCreateGroup = findViewById(R.id.btn_GroupsList_create_group);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_groups);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        RecyclerView rvGroups = findViewById(R.id.rv_groups_list);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        // אתחול האדפטר עם ה-Listener להצטרפות לקבוצה
        groupAdapter = new GroupAdapter(new ArrayList<>(), new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onJoinClick(Group group) {
                handleJoinGroup(group);
            }
        });
        rvGroups.setAdapter(groupAdapter);
    }

    private void setupSearchLogic() {
        // האזנה לשינויים בטקסט החיפוש (שם קבוצה או מיקום)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterGroups(String query) {
        if (query.isEmpty()) {
            updateListDisplay(allGroups);
            return;
        }

        String q = query.toLowerCase();
        List<Group> filteredList = allGroups.stream()
                .filter(group -> group.getName().toLowerCase().contains(q) ||
                        group.getLocation().toLowerCase().contains(q) ||
                        group.getSportType().name().toLowerCase().contains(q))
                .collect(Collectors.toList());

        updateListDisplay(filteredList);
    }

    private void setupCreateGroupButton() {
        btnCreateGroup.setOnClickListener(v -> {
            AddGroupDialog dialog = new AddGroupDialog(this);
            // כשהדיאלוג נסגר, נרענן את הרשימה כדי לראות את הקבוצה החדשה
            dialog.setOnDismissListener(d -> loadGroups());
            dialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getAllGroups(new DatabaseService.DatabaseCallback<List<Group>>() {
            @Override
            public void onCompleted(List<Group> groups) {
                progressBar.setVisibility(View.GONE);
                allGroups = (groups != null) ? groups : new ArrayList<>();
                filterGroups(etSearch.getText().toString());
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load groups", e);
                Toast.makeText(GroupsListActivity.this, "Error loading groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<Group> listToDisplay) {
        // עדכון האדפטר (יש לוודא שיש מתודה setGroupList באדפטר, בדומה ל-UserAdapter)
        // אם אין, ניתן להוסיף אותה או לעדכן את הרשימה הקיימת באדפטר ולקרוא ל-notifyDataSetChanged
        groupAdapter = new GroupAdapter(listToDisplay, group -> handleJoinGroup(group));
        ((RecyclerView) findViewById(R.id.rv_groups_list)).setAdapter(groupAdapter);

        tvGroupCount.setText("Showing " + listToDisplay.size() + " groups");

        if (listToDisplay.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void handleJoinGroup(Group group) {
        String currentUserId = SharedPreferencesUtil.getUserId(this);

        // תיקון: שימוש ב-containsKey במקום contains
        if (group.getMembers() != null && group.getMembers().containsKey(currentUserId)) {
            Toast.makeText(this, "You are already a member of this group", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        databaseService.joinGroup(group.getId(), currentUserId, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show();
                loadGroups(); // רענון הרשימה כדי לראות את השינוי בכמות החברים
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Failed to join group", Toast.LENGTH_SHORT).show();
            }
        });
    }
}