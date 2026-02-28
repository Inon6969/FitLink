package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
    private AutoCompleteTextView etCountryCode;
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
        etCountryCode = findViewById(R.id.et_register_country_code);
        btnRegister = findViewById(R.id.btn_register_register);
        tvLogin = findViewById(R.id.tv_register_login);

        setupCountryCodeDropdown();

        // Set click listeners
        btnRegister.setOnClickListener(this);
        tvLogin.setOnClickListener(this);
    }

    private void setupCountryCodeDropdown() {
        String[] countryCodes = new String[]{
                "+972", // ישראל
                "+1",   // ארה"ב וקנדה
                "+44",  // בריטניה
                "+91",  // הודו
                "+86",  // סין
                "+33",  // צרפת
                "+49",  // גרמניה
                "+34",  // ספרד
                "+39",  // איטליה
                "+55",  // ברזיל
                "+61",  // אוסטרליה
                "+7",   // רוסיה / קזחסטן
                "+52",  // מקסיקו
                "+81",  // יפן
                "+82",  // דרום קוריאה
                "+31",  // הולנד
                "+41",  // שוויץ
                "+46",  // שוודיה
                "+27",  // דרום אפריקה
                "+971", // איחוד האמירויות
                "+65",  // סינגפור
                "+60",  // מלזיה
                "+62",  // אינדונזיה
                "+63",  // הפיליפינים
                "+90"   // טורקיה
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                countryCodes
        );
        etCountryCode.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnRegister.getId()) {
            Log.d(TAG, "Register button clicked");

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String fName = etFName.getText().toString().trim();
            String lName = etLName.getText().toString().trim();

            String prefix = etCountryCode.getText().toString().trim();
            String rawPhone = etPhone.getText().toString().trim();

            // Clean up: If the user typed "050" instead of "50" with a +972 prefix, remove the 0.
            if (rawPhone.startsWith("0")) {
                rawPhone = rawPhone.substring(1);
            }

            String fullPhone = prefix + rawPhone;

            // Validate local input fields
            if (!checkInput(email, password, fName, lName, fullPhone)) {
                return;
            }

            // Start registration process with availability checks
            registerUser(email, password, fName, lName, fullPhone);
        } else if (v.getId() == tvLogin.getId()) {
            // Navigate back to Login Activity
            finish();
        }
    }

    /**
     * Checks if the user input follows basic validation rules.
     */
    private boolean checkInput(String email, String password, String fName, String lName, String fullPhone) {
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

        if (!Validator.isPhoneValid(fullPhone)) {
            etPhone.setError("Invalid phone number format");
            etPhone.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Checks phone and email availability sequentially before creating the user.
     */
    private void registerUser(String email, String password, String fName, String lName, String fullPhone) {
        Log.d(TAG, "Starting registration availability checks...");

        databaseService.checkIfPhoneExists(fullPhone, new DatabaseService.DatabaseCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean phoneExists) {
                if (phoneExists) {
                    Toast.makeText(RegisterActivity.this, "Phone number is already registered", Toast.LENGTH_SHORT).show();
                    etPhone.setError("Already in use");
                    etPhone.requestFocus();
                } else {
                    checkEmailAndRegister(email, password, fName, lName, fullPhone);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to check phone existence", e);
                Toast.makeText(RegisterActivity.this, "Error verifying registration details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkEmailAndRegister(String email, String password, String fName, String lName, String fullPhone) {
        databaseService.checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean emailExists) {
                if (emailExists) {
                    Toast.makeText(RegisterActivity.this, "Email is already registered", Toast.LENGTH_SHORT).show();
                    etEmail.setError("Already in use");
                    etEmail.requestFocus();
                } else {
                    String uid = databaseService.generateUserId();
                    User newUser = new User(uid, email, password, fName, lName, fullPhone, false, null);
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