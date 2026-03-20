package com.example.fitlink.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.fitlink.adapters.ContactMessageAdapter;
import com.example.fitlink.models.ContactMessage;
import com.example.fitlink.services.DatabaseService;

import java.util.List;

public class AdminContactMessagesListActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private ProgressBar progressBar;
    private LinearLayout layoutNoMessages;
    private TextView tvMessagesCount; // הרפרנס לטקסט החדש
    private ContactMessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_contact_messages_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadMessages();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_admin_messages), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvMessages = findViewById(R.id.rv_admin_messages);
        progressBar = findViewById(R.id.progress_admin_messages);
        layoutNoMessages = findViewById(R.id.layout_no_messages);
        tvMessagesCount = findViewById(R.id.tv_admin_messages_count); // קישור הרכיב מהעיצוב
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_admin_messages);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        messageAdapter = new ContactMessageAdapter(new ContactMessageAdapter.OnMessageClickListener() {

            @Override
            public void onMessageClick(ContactMessage message) {
                if (message.getUserId() != null && !message.getUserId().isEmpty()) {
                    Intent intent = new Intent(AdminContactMessagesListActivity.this, UserProfileActivity.class);
                    intent.putExtra("USER_ID", message.getUserId());
                    startActivity(intent);
                } else {
                    Toast.makeText(AdminContactMessagesListActivity.this, "User profile not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onReplyClick(ContactMessage message) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + message.getEmail()));
                intent.putExtra(Intent.EXTRA_SUBJECT, "FitLink Support Response");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(AdminContactMessagesListActivity.this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteClick(ContactMessage message) {
                // קריאה לדיאלוג המעוצב החדש שלנו במקום ה-AlertDialog הרגיל
                new com.example.fitlink.screens.dialogs.DeleteContactMessageDialog(
                        AdminContactMessagesListActivity.this,
                        message,
                        () -> deleteMessage(message.getId())
                ).show();
            }
        });

        rvMessages.setAdapter(messageAdapter);
    }

    private void loadMessages() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getAllContactMessages(new DatabaseService.DatabaseCallback<List<ContactMessage>>() {
            @Override
            public void onCompleted(List<ContactMessage> messages) {
                progressBar.setVisibility(View.GONE);
                if (messages == null || messages.isEmpty()) {
                    layoutNoMessages.setVisibility(View.VISIBLE);
                    rvMessages.setVisibility(View.GONE);
                    tvMessagesCount.setVisibility(View.GONE); // הסתרת הטקסט כשאין הודעות
                } else {
                    layoutNoMessages.setVisibility(View.GONE);
                    rvMessages.setVisibility(View.VISIBLE);
                    tvMessagesCount.setVisibility(View.VISIBLE); // הצגת הטקסט
                    tvMessagesCount.setText("Total messages: " + messages.size()); // עדכון מספר ההודעות
                    messageAdapter.updateList(messages);
                }
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminContactMessagesListActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMessage(String messageId) {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.deleteContactMessage(messageId, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminContactMessagesListActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
                // ה-Listener ב-DatabaseService ירענן את הרשימה אוטומטית כי עשינו addValueEventListener
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminContactMessagesListActivity.this, "Failed to delete message", Toast.LENGTH_SHORT).show();
            }
        });
    }
}