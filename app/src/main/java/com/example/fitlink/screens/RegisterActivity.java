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
 * Activity for registering a new user.
 * Validates user input and checks for existing email or phone number before creation.
 */
public class RegisterActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "RegisterActivity";

    private EditText etEmail, etPassword, etFName, etLName, etPhone;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etFName = findViewById(R.id.et_register_first_name);
        etLName = findViewById(R.id.et_register_last_name);
        etPhone = findViewById(R.id.et_register_phone);
        btnRegister = findViewById(R.id.btn_register_register);
        tvLogin = findViewById(R.id.tv_register_login);

        // Set click listeners
        btnRegister.setOnClickListener(this);
        tvLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRegister.getId()) {
            Log.d(TAG, "Register button clicked");

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String fName = etFName.getText().toString().trim();
            String lName = etLName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            // Validate local input fields
            if (!checkInput(email, password, fName, lName, phone)) {
                return;
            }

            // Start registration process with availability checks
            registerUser(email, password, fName, lName, phone);
        } else if (v.getId() == tvLogin.getId()) {
            // Navigate back to Login Activity
            finish();
        }
    }

    /**
     * Checks if the user input follows basic validation rules.
     */
    private boolean checkInput(String email, String password, String fName, String lName, String phone) {
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("Invalid email address");
            etEmail.requestFocus();
            return false;
        }

        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("Password must be at least 6 characters long");
            etPassword.requestFocus();
            return false;
        }

        if (!Validator.isNameValid(fName)) {
            etFName.setError("First name is too short");
            etFName.requestFocus();
            return false;
        }

        if (!Validator.isNameValid(lName)) {
            etLName.setError("Last name is too short");
            etLName.requestFocus();
            return false;
        }

        if (!Validator.isPhoneValid(phone)) {
            etPhone.setError("Invalid phone number");
            etPhone.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Checks phone and email availability sequentially before creating the user.
     */
    private void registerUser(String email, String password, String fName, String lName, String phone) {
        Log.d(TAG, "Starting registration availability checks...");

        // First: Check if phone number is already registered
        databaseService.checkIfPhoneExists(phone, new DatabaseService.DatabaseCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean phoneExists) {
                if (phoneExists) {
                    Toast.makeText(RegisterActivity.this, "Phone number is already registered", Toast.LENGTH_SHORT).show();
                    etPhone.setError("Already in use");
                    etPhone.requestFocus();
                } else {
                    // Second: If phone is available, check if email is already registered
                    checkEmailAndRegister(email, password, fName, lName, phone);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to check phone existence", e);
                Toast.makeText(RegisterActivity.this, "Error verifying registration details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkEmailAndRegister(String email, String password, String fName, String lName, String phone) {
        databaseService.checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean emailExists) {
                if (emailExists) {
                    Toast.makeText(RegisterActivity.this, "Email is already registered", Toast.LENGTH_SHORT).show();
                    etEmail.setError("Already in use");
                    etEmail.requestFocus();
                } else {
                    // Final: Both available, proceed to create user
                    String uid = databaseService.generateUserId();
                    User newUser = new User(uid, email, password, fName, lName, phone, false, null);
                    createUserInDatabase(newUser);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to check email existence", e);
                Toast.makeText(RegisterActivity.this, "Error verifying registration details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserInDatabase(User user) {
        databaseService.createNewUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "User created successfully in database");

                // Save to local storage
                SharedPreferencesUtil.saveUser(RegisterActivity.this, user);

                // Redirect to MainActivity and clear history
                Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to create user", e);
                Toast.makeText(RegisterActivity.this, "Failed to register user", Toast.LENGTH_SHORT).show();
                SharedPreferencesUtil.signOutUser(RegisterActivity.this);
            }
        });
    }
}