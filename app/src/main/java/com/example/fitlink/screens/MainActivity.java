package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

/// Main activity for Fitlink app
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private MaterialButton btnJoinorCreateGroup;
    private MaterialButton btnJoinorCreateEvent;
    private MaterialButton btnMyGroups;
    private MaterialButton btnMyCalendar;
    private MaterialButton btnAdminPanel;
    private MaterialButton btnToDetailsAboutUser;
    private MaterialButton btnToContact;
    private MaterialButton btnToExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get current user from shared preferences
        User user = SharedPreferencesUtil.getUser(this);
        Log.d(TAG, "User: " + user);

        // Find views
        btnJoinorCreateGroup = findViewById(R.id.btn_join_or_create_group);
        btnJoinorCreateEvent = findViewById(R.id.btn_join_or_create_event);
        btnMyGroups = findViewById(R.id.btn_my_groups);
        btnMyCalendar = findViewById(R.id.btn_my_calendar);
        btnAdminPanel = findViewById(R.id.btn_admin_panel);
        btnToDetailsAboutUser = findViewById(R.id.btn_main_to_DetailsAboutUser);
        btnToContact = findViewById(R.id.btn_main_to_contact);
        btnToExit = findViewById(R.id.btn_main_to_exit);

        // Set click listeners
        btnJoinorCreateGroup.setOnClickListener(this);
        btnJoinorCreateEvent.setOnClickListener(this);
        btnMyGroups.setOnClickListener(this);
        btnMyCalendar.setOnClickListener(this);
        btnAdminPanel.setOnClickListener(this);
        btnToDetailsAboutUser.setOnClickListener(this);
        btnToContact.setOnClickListener(this);
        btnToExit.setOnClickListener(this);

        // Show admin card only if user is admin
        if (user != null && user.getIsAdmin()) {
            findViewById(R.id.admin_card).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.admin_card).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == btnJoinorCreateGroup.getId()) {
            Log.d(TAG, "Join or Create Group clicked");
            startActivity(new Intent(this, GroupsListActivity.class));
            return;
        }
        if (id == btnJoinorCreateEvent.getId()) {
            Log.d(TAG, "Join or Create Event clicked");
            startActivity(new Intent(this, EventsListActivity.class));
            return;
        }

        if (id == btnMyGroups.getId()) {
            Log.d(TAG, "My Groups clicked");
            startActivity(new Intent(this, MyGroupsActivity.class));
            return;
        }

        if (id == btnMyCalendar.getId()) {
            Log.d(TAG, "My Calendar clicked");
            // startActivity(new Intent(this, MyActivitiesActivity.class)); // במידה ויש לך מסך כזה
            return;
        }

        if (id == btnAdminPanel.getId()) {
            Log.d(TAG, "Admin Panel clicked");
            startActivity(new Intent(this, AdminActivity.class));
            return;
        }

        if (id == btnToDetailsAboutUser.getId()) {
            Log.d(TAG, "Account clicked");
            Intent intent = new Intent(this, UserProfileActivity.class);
            // מנקה את המחסנית ומונע כפילויות של מסך הפרופיל
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return;
        }

        if (id == btnToContact.getId()) {
            Log.d(TAG, "Contact clicked");
            Intent intent = new Intent(this, ContactActivity.class);
            // מנקה את המחסנית ומונע כפילויות של מסך יצירת הקשר
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return;
        }

        if (id == btnToExit.getId()) {
            Log.d(TAG, "Sign out clicked");
            logout(); // מתודה שיורשת מ-BaseActivity
        }
    }
}