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
import com.example.fitlink.adapters.JoinRequestAdapter;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestsActivity extends BaseActivity {

    private Group currentGroup;
    private RecyclerView rvRequests;
    private ProgressBar progressBar;
    private LinearLayout layoutNoRequests;
    private JoinRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_join_requests);

        currentGroup = (Group) getIntent().getSerializableExtra("GROUP_EXTRA");
        if (currentGroup == null) {
            Toast.makeText(this, "Group details missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadPendingRequests();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_join_requests), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvRequests = findViewById(R.id.rv_join_requests);
        progressBar = findViewById(R.id.progressBar_requests);
        layoutNoRequests = findViewById(R.id.layout_no_requests);
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

    private void loadPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        layoutNoRequests.setVisibility(View.GONE);

        // מושכים גרסה מעודכנת של הקבוצה
        databaseService.getGroup(currentGroup.getId(), new DatabaseService.DatabaseCallback<Group>() {
            @Override
            public void onCompleted(Group updatedGroup) {
                if (updatedGroup != null) {
                    currentGroup = updatedGroup;
                }

                if (currentGroup.getPendingRequests() == null || currentGroup.getPendingRequests().isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    layoutNoRequests.setVisibility(View.VISIBLE);
                    rvRequests.setVisibility(View.GONE);
                    return;
                }

                // מושכים את כל המשתמשים ומסננים את מי שממתין
                databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
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
                        } else {
                            layoutNoRequests.setVisibility(View.GONE);
                            rvRequests.setVisibility(View.VISIBLE);
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

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(JoinRequestsActivity.this, "Failed to fetch group data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleApprove(User user) {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.approveJoinRequest(currentGroup.getId(), user.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(JoinRequestsActivity.this, "Request approved", Toast.LENGTH_SHORT).show();
                loadPendingRequests(); // טוען מחדש
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
        databaseService.declineJoinRequest(currentGroup.getId(), user.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(JoinRequestsActivity.this, "Request declined", Toast.LENGTH_SHORT).show();
                loadPendingRequests(); // טוען מחדש
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(JoinRequestsActivity.this, "Error declining request", Toast.LENGTH_SHORT).show();
            }
        });
    }
}