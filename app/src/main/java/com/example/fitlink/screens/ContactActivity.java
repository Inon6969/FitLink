package com.example.fitlink.screens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class ContactActivity extends BaseActivity {

    private TextInputEditText etMessage;
    private TextView tvSendingAs;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact);

        // Fetch the logged-in user details
        currentUser = SharedPreferencesUtil.getUser(this);

        initViews();
        setupNavigation();
        setupClickListeners();

        // Display who the message will be sent as
        if (currentUser != null) {
            tvSendingAs.setText("Sending as: " + currentUser.getFullName() + " (" + currentUser.getEmail() + ")");
        } else {
            tvSendingAs.setText("Sending as: Unknown User");
        }
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_contact_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etMessage = findViewById(R.id.et_contact_message);
        tvSendingAs = findViewById(R.id.tv_contact_sending_as);
    }

    private void setupNavigation() {
        Button btnToMain = findViewById(R.id.btn_contact_to_main);
        Button btnToAccount = findViewById(R.id.btn_contact_to_DetailsAboutUser);
        Button btnToExit = findViewById(R.id.btn_contact_to_exit);

        btnToMain.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnToAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnToExit.setOnClickListener(v -> logout());
    }

    private void setupClickListeners() {
        MaterialCardView cardEmail = findViewById(R.id.card_contact_email);
        MaterialCardView cardPhone = findViewById(R.id.card_contact_phone);
        MaterialButton btnSend = findViewById(R.id.btn_contact_send);

        cardEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@fitlink.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request from FitLink App");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        cardPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+972501234567"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open dialer", Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(v -> handleSendMessage());
    }

    private void handleSendMessage() {
        String message = Objects.requireNonNull(etMessage.getText()).toString().trim();

        if (currentUser == null) {
            Toast.makeText(this, "Error: User identity not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.isEmpty()) {
            etMessage.setError("Please enter a message");
            etMessage.requestFocus();
            return;
        }

        // Get the identity dynamically from the SharedPreferences
        String name = currentUser.getFullName();
        String email = currentUser.getEmail();

        databaseService.sendContactMessage(name, email, message, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(ContactActivity.this, "Message sent successfully!", Toast.LENGTH_LONG).show();
                etMessage.setText("");
                etMessage.clearFocus();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ContactActivity.this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}