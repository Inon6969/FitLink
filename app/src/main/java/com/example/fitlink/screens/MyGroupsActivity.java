package com.example.fitlink.screens;

import android.os.Bundle;
import android.util.Log;
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
import com.example.fitlink.adapters.GroupAdapter;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.GroupDescriptionDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyGroupsActivity extends BaseActivity {

    private static final String TAG = "MyGroupsActivity";

    // UI Elements
    private GroupAdapter adapter;
    private TextView tvGroupCount;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private RecyclerView rvMyGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_groups);

        initViews();
        setupToolbar();
        setupRecyclerView();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_my_groups_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvGroupCount = findViewById(R.id.tv_my_group_count);
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

    private void setupRecyclerView() {
        rvMyGroups.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GroupAdapter(new ArrayList<>(), new GroupAdapter.OnGroupClickListener() {
            @Override
            public void onJoinClick(Group group) {
                Toast.makeText(MyGroupsActivity.this, "You are already a member of this group!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGroupClick(Group group) {
                // Instantiating and showing our new clean dialog class
                new GroupDescriptionDialog(MyGroupsActivity.this, group).show();
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
                    updateListDisplay(new ArrayList<>());
                    return;
                }

                Map<String, Boolean> myGroupIds = user.getGroupIds();

                databaseService.getAllGroups(new DatabaseService.DatabaseCallback<>() {
                    @Override
                    public void onCompleted(List<Group> allGroups) {
                        List<Group> myGroups = new ArrayList<>();
                        for (Group group : allGroups) {
                            if (myGroupIds.containsKey(group.getId())) {
                                myGroups.add(group);
                            }
                        }
                        updateListDisplay(myGroups);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        handleError(e);
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                handleError(e);
            }
        });
    }

    private void updateListDisplay(List<Group> listToDisplay) {
        progressBar.setVisibility(View.GONE);

        if (adapter != null) {
            adapter.updateList(listToDisplay);
        }

        tvGroupCount.setText(MessageFormat.format("Showing {0} groups", listToDisplay.size()));

        if (listToDisplay.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void handleError(Exception e) {
        progressBar.setVisibility(View.GONE);
        Log.e(TAG, "Failed to load my groups", e);
        Toast.makeText(MyGroupsActivity.this, "Error loading groups", Toast.LENGTH_SHORT).show();
    }
}