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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.fitlink.screens.dialogs.CreateGroupDialog;
import com.example.fitlink.screens.dialogs.GroupDescriptionDialog;
import com.example.fitlink.screens.dialogs.GroupFilterDialog;
import com.example.fitlink.screens.dialogs.LeaveGroupDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GroupsListActivity extends BaseActivity {

    private static final String TAG = "GroupsListActivity";

    private GroupAdapter groupAdapter;
    private TextView tvGroupCount;
    private EditText etSearch;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnCreateGroup;
    private MaterialButton btnFilter;

    // משתנים לשמירת המצב הנוכחי של הסינון
    private SportType activeSportFilter = null;
    private DifficultyLevel activeLevelFilter = null;
    private String activeLocationFilter = "";

    // אתחול כ-null כדי למנוע העלמה מוקדמת של ה-ProgressBar
    private List<Group> allGroups = null;
    private CreateGroupDialog currentCreateGroupDialog;

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);

                    // עדכון המיקום לדיאלוג היצירה בלבד
                    if (currentCreateGroupDialog != null && currentCreateGroupDialog.isShowing()) {
                        currentCreateGroupDialog.updateLocationDetails(address, lat, lng);
                    }
                }
            }
    );

    public ActivityResultLauncher<Intent> getMapPickerLauncher() { return mapPickerLauncher; }

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
        btnFilter = findViewById(R.id.btn_filter_groups);
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

        String currentUserId = SharedPreferencesUtil.getUserId(this);

        groupAdapter = new GroupAdapter(new ArrayList<>(), true, currentUserId, new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onJoinClick(Group group) { handleJoinGroup(group); }

            @Override
            public void onLeaveClick(Group group) { handleLeaveGroup(group); }

            @Override
            public void onGroupClick(Group group) {
                // פתיחת דיאלוג התיאור בלבד, ללא אפשרות עריכה
                GroupDescriptionDialog dialog = new GroupDescriptionDialog(GroupsListActivity.this, group);
                dialog.show();
            }
        });
        rvGroups.setAdapter(groupAdapter);
    }

    private void setupSearchLogic() {
        // חיפוש טקסטואלי מהיר בשורת החיפוש הכללית (לפי שם קבוצה)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                executeSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // פתיחת חלון הסינון המתקדם
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

    private void executeSearch() {
        if (allGroups == null) return;

        String nameQuery = etSearch.getText().toString().toLowerCase().trim();

        List<Group> filteredList = allGroups.stream().filter(group -> {
            // 1. סינון לפי שם (מתוך שורת החיפוש החיצונית)
            boolean matchesName = nameQuery.isEmpty() ||
                    (group.getName() != null && group.getName().toLowerCase().contains(nameQuery));

            // 2. סינון לפי סוג ספורט (מתוך הדיאלוג)
            boolean matchesSport = activeSportFilter == null ||
                    (group.getSportType() != null && group.getSportType() == activeSportFilter);

            // 3. סינון לפי רמת קושי (מתוך הדיאלוג)
            boolean matchesLevel = activeLevelFilter == null ||
                    (group.getLevel() != null && group.getLevel() == activeLevelFilter);

            // 4. סינון לפי מיקום (מתוך הדיאלוג)
            boolean matchesLocation = activeLocationFilter.isEmpty() ||
                    (group.getLocation() != null && group.getLocation().getAddress() != null &&
                            group.getLocation().getAddress().toLowerCase().contains(activeLocationFilter.toLowerCase()));

            // הקבוצה תוצג רק אם היא עונה על *כל* תנאי הסינון שהוזנו
            return matchesName && matchesSport && matchesLevel && matchesLocation;
        }).collect(Collectors.toList());

        updateListDisplay(filteredList);
    }

    private void setupCreateGroupButton() {
        btnCreateGroup.setOnClickListener(v -> {
            currentCreateGroupDialog = new CreateGroupDialog(this);
            currentCreateGroupDialog.setOnDismissListener(d -> {
                loadGroups();
                currentCreateGroupDialog = null;
            });
            currentCreateGroupDialog.show();
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
                allGroups = (groups != null) ? groups : new ArrayList<>();
                executeSearch();
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Error loading groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<Group> listToDisplay) {
        // מוודא שה-ProgressBar נעלם רק כשהרשימה מוכנה
        progressBar.setVisibility(View.GONE);
        if (groupAdapter != null) groupAdapter.updateList(listToDisplay);
        tvGroupCount.setText(MessageFormat.format("Showing {0} groups", listToDisplay.size()));
        emptyState.setVisibility(listToDisplay.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void handleJoinGroup(Group group) {
        String currentUserId = SharedPreferencesUtil.getUserId(this);

        if (group.getMembers() != null && group.getMembers().containsKey(currentUserId)) {
            Toast.makeText(this, "You are already a member of this group", Toast.LENGTH_SHORT).show();
            return;
        }

        // במקרה שהמשתמש כבר שלח בקשה (Pending) - נפתח את דיאלוג האישור לביטול
        if (group.getPendingRequests() != null && group.getPendingRequests().containsKey(currentUserId)) {
            new com.example.fitlink.screens.dialogs.CancelJoinRequestDialog(this, group, () -> {
                progressBar.setVisibility(View.VISIBLE);
                databaseService.cancelJoinRequest(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(GroupsListActivity.this, "Join request cancelled", Toast.LENGTH_SHORT).show();
                        loadGroups(); // רענון הרשימה לעדכון הכפתור בחזרה ל-JOIN
                    }

                    @Override
                    public void onFailed(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(GroupsListActivity.this, "Failed to cancel request", Toast.LENGTH_SHORT).show();
                    }
                });
            }).show();
            return;
        }

        // אם הוא עדיין לא שלח בקשה - נשלח בקשה חדשה
        progressBar.setVisibility(View.VISIBLE);
        databaseService.requestToJoinGroup(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Join request sent!", Toast.LENGTH_SHORT).show();
                loadGroups();
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupsListActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLeaveGroup(Group group) {
        String currentUserId = SharedPreferencesUtil.getUserId(this);
        boolean isCreator = group.getCreatorId() != null && group.getCreatorId().equals(currentUserId);

        // מונע מיוצר הקבוצה לעזוב ללא מנהל מחליף
        if (isCreator && (group.getManagers() == null || group.getManagers().isEmpty())) {
            Toast.makeText(this, "You must appoint at least one Manager before leaving the group.", Toast.LENGTH_LONG).show();
            return;
        }

        // קריאה לדיאלוג והעברת המזהה של המשתמש הנוכחי
        new LeaveGroupDialog(this, group, currentUserId, () -> {
            progressBar.setVisibility(View.VISIBLE);
            databaseService.leaveGroup(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GroupsListActivity.this, "Left group successfully", Toast.LENGTH_SHORT).show();
                    loadGroups();
                }

                @Override
                public void onFailed(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GroupsListActivity.this, "Failed to leave group", Toast.LENGTH_SHORT).show();
                }
            });
        }).show();
    }
}