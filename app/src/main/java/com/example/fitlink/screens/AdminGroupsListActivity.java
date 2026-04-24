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
import com.example.fitlink.screens.dialogs.CancelJoinRequestDialog;
import com.example.fitlink.screens.dialogs.CreateGroupDialog;
import com.example.fitlink.screens.dialogs.DeleteGroupDialog;
import com.example.fitlink.screens.dialogs.EditGroupDialog;
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

public class AdminGroupsListActivity extends BaseActivity {

    private GroupAdapter groupAdapter;
    private TextView tvCount;
    private EditText etSearch;
    private MaterialButton btnFilter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnCreateGroup;

    private List<Group> allGroups = null;
    private CreateGroupDialog currentCreateGroupDialog;
    private EditGroupDialog currentEditGroupDialog;
    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);

                    // מעדכן את המפה גם ביצירה וגם בעריכה
                    if (currentCreateGroupDialog != null && currentCreateGroupDialog.isShowing()) {
                        currentCreateGroupDialog.updateLocationDetails(address, lat, lng);
                    } else if (currentEditGroupDialog != null && currentEditGroupDialog.isShowing()) {
                        currentEditGroupDialog.updateLocationDetails(address, lat, lng);
                    }
                }
            }
    );
    private String currentUserId;
    // משתנים לשמירת המצב הנוכחי של הסינון
    private SportType activeSportFilter = null;
    private DifficultyLevel activeLevelFilter = null;
    private String activeLocationFilter = "";

    public ActivityResultLauncher<Intent> getMapPickerLauncher() {
        return mapPickerLauncher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_groups_list);

        currentUserId = SharedPreferencesUtil.getUserId(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchLogic();
        setupCreateGroupButton();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_admin_groups_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvCount = findViewById(R.id.tv_admin_group_count);
        etSearch = findViewById(R.id.edit_admin_groups_search);
        btnFilter = findViewById(R.id.btn_filter_admin_groups);
        progressBar = findViewById(R.id.admin_groups_progress_bar);
        emptyState = findViewById(R.id.admin_groups_empty_state);
        btnCreateGroup = findViewById(R.id.btn_admin_create_group);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_admin_groups);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        RecyclerView rvGroups = findViewById(R.id.rv_admin_groups_list);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        groupAdapter = new GroupAdapter(new ArrayList<>(), true, currentUserId, new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onJoinClick(Group group) {
                handleJoinGroup(group);
            }

            @Override
            public void onLeaveClick(Group group) {
                handleLeaveGroup(group);
            }

            @Override
            public void onGroupClick(Group group) {
                // פתיחת דיאלוג התיאור במצב מנהל!
                new GroupDescriptionDialog(AdminGroupsListActivity.this, group, true, new GroupDescriptionDialog.AdminActionsListener() {
                    @Override
                    public void onEdit(Group g) {
                        currentEditGroupDialog = new EditGroupDialog(AdminGroupsListActivity.this, g, updatedGroup -> {
                            loadGroups(); // מרענן את הרשימה אחרי עדכון מוצלח
                        });
                        currentEditGroupDialog.show();
                    }

                    @Override
                    public void onDelete(Group g) {
                        handleDeleteGroup(g);
                    }
                }).show();
            }
        });
        rvGroups.setAdapter(groupAdapter);
    }

    private void handleJoinGroup(Group group) {
        if (group.getMembers() != null && group.getMembers().containsKey(currentUserId)) {
            Toast.makeText(this, "You are already a member of this group", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- הוספת דיאלוג ביטול הבקשה ---
        if (group.getPendingRequests() != null && group.getPendingRequests().containsKey(currentUserId)) {
            new CancelJoinRequestDialog(this, group, () -> {
                progressBar.setVisibility(View.VISIBLE);
                databaseService.cancelJoinRequest(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdminGroupsListActivity.this, "Join request cancelled", Toast.LENGTH_SHORT).show();
                        loadGroups(); // רענון הרשימה לעדכון הכפתור
                    }

                    @Override
                    public void onFailed(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdminGroupsListActivity.this, "Failed to cancel request", Toast.LENGTH_SHORT).show();
                    }
                });
            }).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        databaseService.requestToJoinGroup(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminGroupsListActivity.this, "Join request sent!", Toast.LENGTH_SHORT).show();
                loadGroups();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminGroupsListActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLeaveGroup(Group group) {
        boolean isCreator = group.getCreatorId() != null && group.getCreatorId().equals(currentUserId);

        if (isCreator && (group.getManagers() == null || group.getManagers().isEmpty())) {
            Toast.makeText(this, "You must appoint at least one Manager before leaving the group.", Toast.LENGTH_LONG).show();
            return;
        }

        new LeaveGroupDialog(this, group, currentUserId, () -> {
            progressBar.setVisibility(View.VISIBLE);
            databaseService.leaveGroup(group.getId(), Objects.requireNonNull(currentUserId), new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminGroupsListActivity.this, "Left group successfully", Toast.LENGTH_SHORT).show();
                    loadGroups();
                }

                @Override
                public void onFailed(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminGroupsListActivity.this, "Failed to leave group", Toast.LENGTH_SHORT).show();
                }
            });
        }).show();
    }

    private void handleDeleteGroup(Group group) {
        new DeleteGroupDialog(this, () -> {
            progressBar.setVisibility(View.VISIBLE);
            databaseService.deleteGroup(group.getId(), new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminGroupsListActivity.this, "Group deleted.", Toast.LENGTH_SHORT).show();
                    loadGroups();
                }

                @Override
                public void onFailed(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminGroupsListActivity.this, "Failed to delete group.", Toast.LENGTH_SHORT).show();
                }
            });
        }).show();
    }

    private void setupSearchLogic() {
        // חיפוש טקסטואלי מהיר בשורת החיפוש הכללית (לפי שם קבוצה)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                executeSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
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
                Toast.makeText(AdminGroupsListActivity.this, "Error loading groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<Group> listToDisplay) {
        progressBar.setVisibility(View.GONE);
        if (groupAdapter != null) groupAdapter.updateList(listToDisplay);
        tvCount.setText(MessageFormat.format("Total groups: {0}", listToDisplay.size()));
        emptyState.setVisibility(listToDisplay.isEmpty() ? View.VISIBLE : View.GONE);
    }
}