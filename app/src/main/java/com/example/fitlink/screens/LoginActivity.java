package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.example.fitlink.utils.Validator;

/**
 * Activity for logging in the user.
 * Contains fields for the user to enter their email and password,
 * and a button to log in. Upon successful login, redirects to MainActivity.
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Adjust padding for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login_login);
        tvRegister = findViewById(R.id.tv_login_register);
    }

    private void setupListeners() {
        // Modern Lambda expressions instead of implementing View.OnClickListener globally
        btnLogin.setOnClickListener(v -> handleLogin());

        tvRegister.setOnClickListener(v -> {
            Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(registerIntent);
        });
    }

    private void handleLogin() {
        Log.d(TAG, "handleLogin: Login button clicked");

        // Get inputs and trim email to prevent trailing/leading space issues
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        Log.d(TAG, "handleLogin: Validating input...");

        if (!checkInput(email, password)) {
            return;
        }

        Log.d(TAG, "handleLogin: Logging in user...");
        loginUser(email, password);
    }

    /**
     * Checks if the email and password inputs are valid.
     * @return true if valid, false otherwise.
     */
    private boolean checkInput(String email, String password) {
        if (!Validator.isEmailValid(email)) {
            Log.e(TAG, "checkInput: Invalid email address");
            etEmail.setError("Invalid email address");
            etEmail.requestFocus();
            return false;
        }

        if (!Validator.isPasswordValid(password)) {
            Log.e(TAG, "checkInput: Invalid password");
            etPassword.setError("Password must be at least 6 characters long");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {
        setLoadingState(true); // Disable button to prevent multiple clicks

        databaseService.getUserByEmailAndPassword(email, password, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                setLoadingState(false);

                if (user == null) {
                    etPassword.setError("Invalid email or password");
                    etPassword.requestFocus();
                    return;
                }

                Log.d(TAG, "onCompleted: User logged in: " + user.getId());

                // Save the user data to shared preferences
                SharedPreferencesUtil.saveUser(LoginActivity.this, user);

                // Redirect to main activity and clear history
                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
            }

            @Override
            public void onFailed(Exception e) {
                setLoadingState(false);
                Log.e(TAG, "onFailed: Failed to retrieve user data", e);

                etPassword.setError("Invalid email or password");
                etPassword.requestFocus();

                // Sign out just in case there's corrupted local data
                SharedPreferencesUtil.signOutUser(LoginActivity.this);
            }
        });
    }

    /**
     * Toggles the UI state during a network call to improve UX.
     */
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");
            btnLogin.setAlpha(0.7f); // Make it look disabled
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
            btnLogin.setAlpha(1.0f);
        }
    }
}