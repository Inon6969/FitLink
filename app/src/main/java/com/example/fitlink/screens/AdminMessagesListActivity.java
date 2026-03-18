package com.example.fitlink.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

public class AdminMessagesListActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private ProgressBar progressBar;
    private LinearLayout layoutNoMessages;
    private ContactMessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_messages_list);

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
            public void onReplyClick(ContactMessage message) {
                // פתיחת אפליקציית אימייל עם כתובת המשתמש
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + message.getEmail()));
                intent.putExtra(Intent.EXTRA_SUBJECT, "FitLink Support Response");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(AdminMessagesListActivity.this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteClick(ContactMessage message) {
                // דיאלוג אישור לפני מחיקה
                new AlertDialog.Builder(AdminMessagesListActivity.this)
                        .setTitle("Delete Message")
                        .setMessage("Are you sure you want to delete this message?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message.getId()))
                        .setNegativeButton("Cancel", null)
                        .show();
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
                } else {
                    layoutNoMessages.setVisibility(View.GONE);
                    rvMessages.setVisibility(View.VISIBLE);
                    messageAdapter.updateList(messages);
                }
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminMessagesListActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMessage(String messageId) {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.deleteContactMessage(messageId, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminMessagesListActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
                // ה-Listener ב-DatabaseService ירענן את הרשימה אוטומטית כי עשינו addValueEventListener
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminMessagesListActivity.this, "Failed to delete message", Toast.LENGTH_SHORT).show();
            }
        });
    }
}