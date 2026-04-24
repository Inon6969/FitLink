package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.example.fitlink.adapters.UserAdapter;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MembersListActivity extends BaseActivity {

    private Group currentGroup;
    private RecyclerView rvMembers;
    private ProgressBar progressBar;
    private LinearLayout layoutNoMembers;
    private TextView tvMembersCount;

    private UserAdapter userAdapter;
    private String currentUserId;
    private boolean isGroupCreator = false;
    private boolean isGroupManager = false;

    private ValueEventListener groupListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_members_list);

        String groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = SharedPreferencesUtil.getUserId(this);

        // האזנה בזמן אמת לקבוצה
        groupListener = DatabaseService.getInstance().listenToGroup(groupId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Group group) {
                if (group == null) {
                    Toast.makeText(MembersListActivity.this, "Group was deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // העפה מיידית אם המשתמש כבר לא חלק מהקבוצה
                if (group.getMembers() == null || !group.getMembers().containsKey(currentUserId)) {
                    Toast.makeText(MembersListActivity.this, "You are no longer a member of this group.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                currentGroup = group;
                isGroupCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
                isGroupManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(currentUserId);

                if (rvMembers == null) {
                    // פעם ראשונה שהנתונים נטענו
                    continueInitialization();
                } else {
                    // עדכון התרחש בזמן שהמשתמש במסך
                    if (userAdapter != null) {
                        userAdapter.setGroupMode(true, isGroupCreator, isGroupManager, currentGroup.getCreatorId(), currentGroup.getManagers());
                    }
                    loadGroupMembers(); // משיכת חברי הקבוצה המעודכנים
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(MembersListActivity.this, "Failed to load group details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId != null && groupListener != null) {
            DatabaseService.getInstance().removeGroupListener(groupId, groupListener);
        }
    }

    private void continueInitialization() {
        initViews();
        setupToolbar();
        setupRecyclerView();
        loadGroupMembers();
    }

    private void initViews() {
        View root = findViewById(R.id.main_members_list);
        AppBarLayout appBarLayout = findViewById(R.id.toolbar_members_list).getParent() instanceof AppBarLayout
                ? (AppBarLayout) findViewById(R.id.toolbar_members_list).getParent()
                : null;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            if (appBarLayout != null) {
                appBarLayout.setPadding(0, systemBars.top, 0, 0);
            }
            return insets;
        });

        root.post(() -> ViewCompat.requestApplyInsets(root));

        rvMembers = findViewById(R.id.rv_members_list);
        progressBar = findViewById(R.id.progressBar_members);
        layoutNoMembers = findViewById(R.id.layout_no_members);
        tvMembersCount = findViewById(R.id.tv_members_count);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_members_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentGroup.getName() + " Members");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(MembersListActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }

            @Override
            public void onEditUser(User user) {
            }

            @Override
            public void onToggleAdmin(User user) {
                handleToggleManager(user);
            }

            @Override
            public void onDeleteUser(User user) {
                handleRemoveMember(user);
            }

            @Override
            public boolean isCurrentUser(User user) {
                return user.getId().equals(currentUserId);
            }
        });

        userAdapter.setGroupMode(true, isGroupCreator, isGroupManager, currentGroup.getCreatorId(), currentGroup.getManagers());
        rvMembers.setAdapter(userAdapter);
    }

    private void handleToggleManager(User user) {
        boolean isCurrentlyManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(user.getId());
        boolean newStatus = !isCurrentlyManager;

        progressBar.setVisibility(View.VISIBLE);
        databaseService.updateGroupManager(currentGroup.getId(), user.getId(), newStatus, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(MembersListActivity.this, newStatus ? "Manager added" : "Manager removed", Toast.LENGTH_SHORT).show();
                // הנתונים יתרעננו מעצמם בגלל ה-Listener
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembersListActivity.this, "Failed to update manager status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ה-Listener דואג לעדכון הקבוצה, אז כאן אנחנו שואבים רק את רשימת המשתמשים כדי לסננן מתוכה את החברים
    private void loadGroupMembers() {
        progressBar.setVisibility(View.VISIBLE);
        layoutNoMembers.setVisibility(View.GONE);
        tvMembersCount.setVisibility(View.GONE);

        databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> allUsers) {
                progressBar.setVisibility(View.GONE);
                List<User> groupMembers = new ArrayList<>();

                if (allUsers != null && currentGroup.getMembers() != null) {
                    for (User user : allUsers) {
                        if (currentGroup.getMembers().containsKey(user.getId())) {
                            groupMembers.add(user);
                        }
                    }
                }

                if (groupMembers.isEmpty()) {
                    layoutNoMembers.setVisibility(View.VISIBLE);
                    rvMembers.setVisibility(View.GONE);
                    tvMembersCount.setVisibility(View.GONE);
                } else {
                    layoutNoMembers.setVisibility(View.GONE);
                    rvMembers.setVisibility(View.VISIBLE);
                    tvMembersCount.setVisibility(View.VISIBLE);
                    tvMembersCount.setText("Total members: " + groupMembers.size());
                    userAdapter.setUserList(groupMembers);
                }
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembersListActivity.this, "Failed to load members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRemoveMember(User user) {
        if (currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(user.getId())) {
            Toast.makeText(this, "Cannot remove the group creator", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGroupManager && !isGroupCreator && currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(user.getId())) {
            Toast.makeText(this, "Managers cannot remove other managers", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        databaseService.leaveGroup(currentGroup.getId(), user.getId(), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(MembersListActivity.this, "Member removed", Toast.LENGTH_SHORT).show();
                // הנתונים יתרעננו מעצמם בגלל ה-Listener
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembersListActivity.this, "Failed to remove member", Toast.LENGTH_SHORT).show();
            }
        });
    }
}