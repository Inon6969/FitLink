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
import com.example.fitlink.adapters.JoinRequestAdapter;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestsActivity extends BaseActivity {

    private Group currentGroup;
    private RecyclerView rvRequests;
    private ProgressBar progressBar;
    private LinearLayout layoutNoRequests;
    private TextView tvRequestsCount;
    private JoinRequestAdapter adapter;

    private String currentUserId;
    private ValueEventListener groupListener;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join_requests);

        String groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = SharedPreferencesUtil.getUserId(this);

        // מאזין זמן אמת לקבוצה - מתעדכן אוטומטית ומעיף את המשתמש אם איבד הרשאות
        groupListener = DatabaseService.getInstance().listenToGroup(groupId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Group group) {
                if (group == null) {
                    Toast.makeText(JoinRequestsActivity.this, "This group no longer exists.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // בדיקת הרשאות: רק יוצר הקבוצה ומנהלים מורשים לראות את המסך הזה
                boolean isCreator = group.getCreatorId() != null && group.getCreatorId().equals(currentUserId);
                boolean isManager = group.getManagers() != null && group.getManagers().containsKey(currentUserId);

                if (!isCreator && !isManager) {
                    Toast.makeText(JoinRequestsActivity.this, "You no longer have permission to view requests.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                currentGroup = group;

                if (!isInitialized) {
                    isInitialized = true;
                    continueInitialization();
                } else {
                    // הנתונים השתנו (למשל נוספה בקשה חדשה) - נרענן את הרשימה
                    loadPendingRequests();
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(JoinRequestsActivity.this, "Failed to load group details", Toast.LENGTH_SHORT).show();
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
        loadPendingRequests();
    }

    private void initViews() {
        View root = findViewById(R.id.main_join_requests);
        AppBarLayout appBarLayout = findViewById(R.id.toolbar_join_requests).getParent() instanceof AppBarLayout
                ? (AppBarLayout) findViewById(R.id.toolbar_join_requests).getParent()
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

        rvRequests = findViewById(R.id.rv_join_requests);
        progressBar = findViewById(R.id.progressBar_requests);
        layoutNoRequests = findViewById(R.id.layout_no_requests);
        tvRequestsCount = findViewById(R.id.tv_requests_count);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_join_requests);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentGroup.getName() + " Join Requests");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JoinRequestAdapter(new ArrayList<>(), new JoinRequestAdapter.OnRequestClickListener() {

            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(JoinRequestsActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }

            @Override
            public void onApproveClick(User user) {
                handleApprove(user);
            }

            @Override
            public void onDeclineClick(User user) {
                handleDecline(user);
            }
        });
        rvRequests.setAdapter(adapter);
    }

    // הפונקציה קוצרה ויועלה מכיוון ש-currentGroup כבר מעודכן תמיד בזכות ה-Listener
    private void loadPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        layoutNoRequests.setVisibility(View.GONE);
        tvRequestsCount.setVisibility(View.GONE);

        if (currentGroup.getPendingRequests() == null || currentGroup.getPendingRequests().isEmpty()) {
            progressBar.setVisibility(View.GONE);
            layoutNoRequests.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
            tvRequestsCount.setVisibility(View.GONE);
            return;
        }

        databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> allUsers) {
                progressBar.setVisibility(View.GONE);
                List<User> pendingUsers = new ArrayList<>();

                if (allUsers != null) {
                    for (User user : allUsers) {
                        if (currentGroup.getPendingRequests().containsKey(user.getId())) {
                            pendingUsers.add(user);
                        }
                    }
                }

                if (pendingUsers.isEmpty()) {
                    layoutNoRequests.setVisibility(View.VISIBLE);
                    rvRequests.setVisibility(View.GONE);
                    tvRequestsCount.setVisibility(View.GONE);
                } else {
                    layoutNoRequests.setVisibility(View.GONE);
                    rvRequests.setVisibility(View.VISIBLE);

                    tvRequestsCount.setVisibility(View.VISIBLE);
                    tvRequestsCount.setText("Total requests: " + pendingUsers.size());

                    adapter.updateList(pendingUsers);
                }
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(JoinRequestsActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleApprove(User user) {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.approveJoinRequest(currentGroup.getId(), user.getId(), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(JoinRequestsActivity.this, "Request approved", Toast.LENGTH_SHORT).show();
                // אין צורך לקרוא ל-loadPendingRequests() ידנית, ה-Listener יזהה את השינוי וירענן אוטומטית
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(JoinRequestsActivity.this, "Error approving request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDecline(User user) {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.declineJoinRequest(currentGroup.getId(), user.getId(), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(JoinRequestsActivity.this, "Request declined", Toast.LENGTH_SHORT).show();
                // ה-Listener ירענן את הרשימה באופן אוטומטי
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(JoinRequestsActivity.this, "Error declining request", Toast.LENGTH_SHORT).show();
            }
        });
    }
}