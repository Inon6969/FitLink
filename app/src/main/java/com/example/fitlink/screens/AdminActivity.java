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

public class AdminActivity extends BaseActivity {

    LinearLayout cardUsers, cardGroups, cardEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        initViews();
        setupToolbar();
        setupClickListeners();
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
        // ניהול משתמשים
        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminUsersListActivity.class);
            startActivity(intent);
        });

        // ניהול קבוצות
        cardGroups.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminGroupsListActivity.class);
            startActivity(intent);
        });

        // ניהול אירועים
        cardEvents.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEventsListActivity.class); // שינינו ל-AdminEventsListActivity
            startActivity(intent);
        });
    }
}