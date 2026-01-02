package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;

/// Landing activity for the app
/// This activity is the first activity that is shown when the app is first opened (when the user is not signed in)
/// It contains buttons to navigate to the login and register activities
public class LandingActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "LandingActivity";

    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        /// set the layout for the activity
        setContentView(R.layout.activity_landing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        User current = SharedPreferencesUtil.getUser(this);
        if (SharedPreferencesUtil.isUserLoggedIn(this)) {
            databaseService.getUser(current.getId(), new DatabaseService.DatabaseCallback<User>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null) {
                        Intent intent = new Intent(LandingActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }

                @Override
                public void onFailed(Exception e) {
                }
            });
        }

        /// get the views
        btnLogin = findViewById(R.id.btn_landing_login);
        btnRegister = findViewById(R.id.btn_landing_register);

        /// set the click listeners
        btnLogin.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnLogin.getId()) {
            Log.d(TAG, "onClick: Login button clicked");
            Intent loginIntent = new Intent(LandingActivity.this, LoginActivity.class);
            startActivity(loginIntent);
        } else if (v.getId() == btnRegister.getId()) {
            Log.d(TAG, "onClick: Register button clicked");
            Intent registerIntent = new Intent(LandingActivity.this, RegisterActivity.class);
            startActivity(registerIntent);
        }
    }
}