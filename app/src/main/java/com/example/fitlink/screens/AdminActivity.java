package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.ui.AdminStatsGraphView;

import java.util.List;

public class AdminActivity extends BaseActivity {

    LinearLayout cardUsers, cardGroups, cardEvents, cardMessages;
    AdminStatsGraphView statsGraphView;
    DatabaseService databaseService;

    // משתנים לשמירת הכמויות
    private int userCount = 0;
    private int groupCount = 0;
    private int eventCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        databaseService = DatabaseService.getInstance();

        initViews();
        setupToolbar();
        setupClickListeners();

        // טעינת נתונים לגרף
        loadStatistics();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cardUsers = findViewById(R.id.card_users);
        cardGroups = findViewById(R.id.card_groups);
        cardEvents = findViewById(R.id.card_events);
        cardMessages = findViewById(R.id.card_messages);

        // אתחול הגרף
        statsGraphView = findViewById(R.id.admin_stats_graph);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupClickListeners() {
        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminUsersListActivity.class);
            startActivity(intent);
        });

        cardGroups.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminGroupsListActivity.class);
            startActivity(intent);
        });

        cardEvents.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEventsListActivity.class);
            startActivity(intent);
        });

        cardMessages.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminContactMessagesListActivity.class);
            startActivity(intent);
        });
    }

    // פונקציה חדשה לאיסוף הנתונים
    private void loadStatistics() {
        // משיכת כמות המשתמשים
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> object) {
                if (object != null) {
                    userCount = object.size();
                    updateGraph();
                }
            }

            @Override
            public void onFailed(Exception e) {
            }
        });

        // משיכת כמות הקבוצות
        databaseService.getAllGroups(new DatabaseService.DatabaseCallback<List<Group>>() {
            @Override
            public void onCompleted(List<Group> object) {
                if (object != null) {
                    groupCount = object.size();
                    updateGraph();
                }
            }

            @Override
            public void onFailed(Exception e) {
            }
        });

        // משיכת כמות האירועים
        databaseService.getAllEvents(new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> object) {
                if (object != null) {
                    eventCount = object.size();
                    updateGraph();
                }
            }

            @Override
            public void onFailed(Exception e) {
            }
        });
    }

    // עדכון ה-Custom View
    private void updateGraph() {
        if (statsGraphView != null) {
            statsGraphView.setStats(userCount, groupCount, eventCount);
        }
    }
}