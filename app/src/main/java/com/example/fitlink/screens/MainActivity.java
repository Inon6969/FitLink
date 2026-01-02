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

    private MaterialButton btnJoinRun, btnCreateRide, btnMyActivities, btnEditProfile, btnAdminPanel, btnToContact, btnToDetailsAboutUser, btnToExit;

    private User user;

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
        user = SharedPreferencesUtil.getUser(this);
        Log.d(TAG, "User: " + user);

        // Find views
        btnJoinRun = findViewById(R.id.btn_join_run);
        btnCreateRide = findViewById(R.id.btn_create_ride);
        btnMyActivities = findViewById(R.id.btn_my_activities);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnAdminPanel = findViewById(R.id.btn_admin_panel);
        btnToContact = findViewById(R.id.btn_main_to_contact);
        btnToDetailsAboutUser = findViewById(R.id.btn_main_to_DetailsAboutUser);
        btnToExit = findViewById(R.id.btn_main_to_exit);

        // Set click listeners
        btnJoinRun.setOnClickListener(this);
        btnCreateRide.setOnClickListener(this);
        btnMyActivities.setOnClickListener(this);
        btnEditProfile.setOnClickListener(this);
        btnAdminPanel.setOnClickListener(this);
        btnToContact.setOnClickListener(this);
        btnToDetailsAboutUser.setOnClickListener(this);
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
        /*
        if (v.getId() == btnJoinRun.getId()) {
            Log.d(TAG, "Join Run clicked");
            startActivity(new Intent(this, JoinRunActivity.class));
            return;
        }
        if (v.getId() == btnCreateRide.getId()) {
            Log.d(TAG, "Create Ride clicked");
            startActivity(new Intent(this, CreateRideActivity.class));
            return;
        }
        if (v.getId() == btnMyActivities.getId()) {
            Log.d(TAG, "My Activities clicked");
            startActivity(new Intent(this, MyActivitiesActivity.class));
            return;
        }
        if (v.getId() == btnEditProfile.getId()) {
            Log.d(TAG, "Edit Profile clicked");
            startActivity(new Intent(this, UserProfileActivity.class));
            return;
        }

        if (v.getId() == btnToContact.getId()) {
            Log.d(TAG, "Contact clicked");
            startActivity(new Intent(this, JoinRunActivity.class));
            return;
        }
         */
        if (v.getId() == btnAdminPanel.getId()) {
            Log.d(TAG, "Admin Panel clicked");
            startActivity(new Intent(this, AdminActivity.class));
        }
        if (v.getId() == btnToDetailsAboutUser.getId()) {
            Log.d(TAG, "Account clicked");
            startActivity(new Intent(this, UserProfileActivity.class));
            return;
        }
        if (v.getId() == btnToExit.getId()) {
            Log.d(TAG, "Sign out clicked");
            logout();
            return;
        }
    }
}