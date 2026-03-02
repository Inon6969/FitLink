package com.example.fitlink.screens;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.adapters.ChatAdapter;
import com.example.fitlink.models.ChatMessage;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GroupChatActivity extends BaseActivity {

    private Group currentGroup;
    private RecyclerView rvChat;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private String currentUserId;
    private String currentUserName = "Unknown"; // ישלוף ממסד הנתונים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_chat);

        currentGroup = (Group) getIntent().getSerializableExtra("GROUP_EXTRA");
        if (currentGroup == null) {
            Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = SharedPreferencesUtil.getUserId(this);
        messageList = new ArrayList<>();

        initViews();
        fetchUserDetailsAndListenToChat();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_group_chat), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar_group_chat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentGroup.getName() + " Chat");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        rvChat = findViewById(R.id.rv_chat_messages);
        etMessage = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.fab_send_message);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // גורם להודעות להתחיל מלמטה
        rvChat.setLayoutManager(layoutManager);

        // בדיקה האם המשתמש הנוכחי הוא יוצר הקבוצה או מנהל
        boolean isCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
        boolean isManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(currentUserId);

        adapter = new ChatAdapter(messageList, currentUserId, currentGroup.getCreatorId(), currentGroup.getManagers(), message -> {
            // לחיצה ארוכה - בדיקת הרשאות מחיקה ליוצר הקבוצה ולמנהלים
            if (isCreator || isManager) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Message")
                        .setMessage("Are you sure you want to delete this message?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteMessage(message))
                        .setNegativeButton("No", null)
                        .show();
            } else {
                Toast.makeText(this, "Only the group creator and managers can delete messages.", Toast.LENGTH_SHORT).show();
            }
        });
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void fetchUserDetailsAndListenToChat() {
        DatabaseService.getInstance().getUser(currentUserId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null) {
                    currentUserName = user.getFirstName() + " " + user.getLastName();
                }
                listenToMessages();
            }

            @Override
            public void onFailed(Exception e) {
                listenToMessages();
            }
        });
    }

    private void listenToMessages() {
        DatabaseService.getInstance().listenForGroupMessages(currentGroup.getId(), new DatabaseService.DatabaseCallback<List<ChatMessage>>() {
            @Override
            public void onCompleted(List<ChatMessage> messages) {
                messageList.clear();
                messageList.addAll(messages);
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        ChatMessage message = new ChatMessage(currentUserId, currentUserName, text, System.currentTimeMillis());

        // מנקים את שורת הטקסט ישר
        etMessage.setText("");

        DatabaseService.getInstance().sendGroupMessage(currentGroup.getId(), message, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                // נשלח בהצלחה, המאזין כבר יעדכן את הרשימה
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMessage(ChatMessage message) {
        DatabaseService.getInstance().deleteGroupMessage(currentGroup.getId(), message.getMessageId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(GroupChatActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupChatActivity.this, "Failed to delete message", Toast.LENGTH_SHORT).show();
            }
        });
    }
}