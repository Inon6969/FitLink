package com.example.fitlink.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import java.util.ArrayList;
import java.util.List;

public class MembersListActivity extends BaseActivity {

    private Group currentGroup;
    private RecyclerView rvMembers;
    private ProgressBar progressBar;
    private LinearLayout layoutNoMembers;

    private UserAdapter userAdapter;
    private String currentUserId;
    private boolean isGroupAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_members_list);

        currentGroup = (Group) getIntent().getSerializableExtra("GROUP_EXTRA");
        if (currentGroup == null) {
            Toast.makeText(this, "Group details missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = SharedPreferencesUtil.getUserId(this);

        // בדיקה האם המשתמש שנכנס למסך הוא המנהל של הקבוצה
        isGroupAdmin = currentGroup.getAdminId() != null && currentGroup.getAdminId().equals(currentUserId);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadGroupMembers();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_members_list), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvMembers = findViewById(R.id.rv_members_list);
        progressBar = findViewById(R.id.progressBar_members);
        layoutNoMembers = findViewById(R.id.layout_no_members);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_members_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Members (" + currentGroup.getName() + ")");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // אופציונלי: צפייה בפרופיל
            }

            @Override
            public void onToggleAdmin(User user) {
                // ויתרנו על הוספת מנהלים, ולכן הפונקציה הזו לא תעשה כלום במסך הזה.
            }

            @Override
            public void onDeleteUser(User user) {
                // כפתור המחיקה ישמש להסרה מהקבוצה
                handleRemoveMember(user);
            }

            @Override
            public boolean isCurrentUser(User user) {
                return user.getId().equals(currentUserId);
            }
        });

        // כאן אנחנו מעבירים ל-Adapter מצב מיוחד שמסתיר את כפתורי מנהל-העל (ראה שלב 3)
        userAdapter.setGroupMode(true, isGroupAdmin);
        rvMembers.setAdapter(userAdapter);
    }

    private void loadGroupMembers() {
        progressBar.setVisibility(View.VISIBLE);
        layoutNoMembers.setVisibility(View.GONE);

        // 1. קודם כל שולפים את הגרסה המעודכנת ביותר של הקבוצה ישירות מהשרת!
        databaseService.getGroup(currentGroup.getId(), new DatabaseService.DatabaseCallback<Group>() {
            @Override
            public void onCompleted(Group updatedGroup) {
                if (updatedGroup != null) {
                    currentGroup = updatedGroup; // מעדכנים את האובייקט המקומי לגרסה הטרייה
                }

                // 2. רק עכשיו שולפים את רשימת המשתמשים ומסננים לפי המילון המעודכן
                databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
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
                        } else {
                            layoutNoMembers.setVisibility(View.GONE);
                            rvMembers.setVisibility(View.VISIBLE);
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

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembersListActivity.this, "Failed to sync group data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRemoveMember(User user) {
        // המנהל לא יכול להסיר את עצמו דרך הכפתור הזה
        if (currentGroup.getAdminId().equals(user.getId())) {
            Toast.makeText(this, "Cannot remove the group creator", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        // שימוש בפונקציה leaveGroup כדי להסיר משתמש מהקבוצה
        databaseService.leaveGroup(currentGroup.getId(), user.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(MembersListActivity.this, "Member removed", Toast.LENGTH_SHORT).show();
                currentGroup.getMembers().remove(user.getId());
                loadGroupMembers(); // רענון הרשימה לאחר מחיקה מוצלחת
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembersListActivity.this, "Failed to remove member", Toast.LENGTH_SHORT).show();
            }
        });
    }
}