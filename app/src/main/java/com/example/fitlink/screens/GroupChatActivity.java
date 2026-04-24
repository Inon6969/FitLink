package com.example.fitlink.screens;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.fitlink.utils.ImageUtil;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupChatActivity extends BaseActivity {

    // משתנה סטטי לשמירת מזהה הקבוצה הפעילה כרגע כדי למנוע התראות כפולות בזמן צ'אט
    public static String activeGroupId = null;

    private Group currentGroup;
    private RecyclerView rvChat;
    private EditText etMessage;
    private MaterialButton btnSend;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private String currentUserId;
    private String currentUserName = "Unknown";

    private ValueEventListener groupListener;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_chat);

        String groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = SharedPreferencesUtil.getUserId(this);

        // מאזין זמן אמת לקבוצה - אם המשתמש מוסר, הוא נזרק מהצ'אט
        groupListener = DatabaseService.getInstance().listenToGroup(groupId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Group group) {
                if (group == null) {
                    Toast.makeText(GroupChatActivity.this, "This group no longer exists.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // העפה מיידית אם המשתמש כבר לא ברשימת המשתתפים
                if (group.getMembers() == null || !group.getMembers().containsKey(currentUserId)) {
                    Toast.makeText(GroupChatActivity.this, "You are no longer a member of this group.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                currentGroup = group;

                if (!isInitialized) {
                    isInitialized = true;
                    continueInitialization();
                } else {
                    // עדכון הרשאות דינמי בזמן אמת (למקרה שמישהו מונה או הוסר מניהול בזמן שהוא בצ'אט)
                    if (adapter != null) {
                        adapter.updateGroupManagers(currentGroup.getManagers());
                    }
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupChatActivity.this, "Failed to load group details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // כשהמסך מוצג למשתמש, נשמור איזה צ'אט פתוח
        activeGroupId = getIntent().getStringExtra("GROUP_ID");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // כשהמשתמש יוצא מהמסך, נאפס את המשתנה
        activeGroupId = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId != null && groupListener != null) {
            DatabaseService.getInstance().removeGroupListener(groupId, groupListener);
        }
    }

    private void continueInitialization() {
        messageList = new ArrayList<>();
        initViews();
        fetchUserDetailsAndListenToChat();
    }

    private void initViews() {
        View root = findViewById(R.id.main_group_chat);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemAndImeBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemAndImeBars.left, systemAndImeBars.top, systemAndImeBars.right, systemAndImeBars.bottom);
            return insets;
        });

        root.post(() -> ViewCompat.requestApplyInsets(root));

        Toolbar toolbar = findViewById(R.id.toolbar_group_chat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentGroup.getName() + " Chat");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        rvChat = findViewById(R.id.rv_chat_messages);
        etMessage = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_send_message);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);

        boolean isCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
        boolean isManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(currentUserId);

        adapter = new ChatAdapter(messageList, currentUserId, currentGroup.getCreatorId(), currentGroup.getManagers(), new ChatAdapter.OnMessageClickListener() {
            @Override
            public void onMessageLongClick(ChatMessage message) {
                // בדיקה מחודשת של הרשאות בעת לחיצה, למקרה שהשתנו
                boolean currentIsCreator = currentGroup.getCreatorId() != null && currentGroup.getCreatorId().equals(currentUserId);
                boolean currentIsManager = currentGroup.getManagers() != null && currentGroup.getManagers().containsKey(currentUserId);

                if (currentIsCreator || currentIsManager) {
                    new AlertDialog.Builder(GroupChatActivity.this)
                            .setTitle("Delete Message")
                            .setMessage("Are you sure you want to delete this message?")
                            .setPositiveButton("Yes", (dialog, which) -> deleteMessage(message))
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    Toast.makeText(GroupChatActivity.this, "Only the group creator and managers can delete messages.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNameClick(ChatMessage message) {
                Intent intent = new Intent(GroupChatActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", message.getSenderId());
                startActivity(intent);
            }

            @Override
            public void onImageClick(ChatMessage message) {
                showFullImageDialog(message.getSenderId());
            }
        });
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void fetchUserDetailsAndListenToChat() {
        DatabaseService.getInstance().getUser(currentUserId, new DatabaseService.DatabaseCallback<>() {
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
        DatabaseService.getInstance().listenForGroupMessages(currentGroup.getId(), new DatabaseService.DatabaseCallback<>() {
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
        etMessage.setText("");

        DatabaseService.getInstance().sendGroupMessage(currentGroup.getId(), message, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMessage(ChatMessage message) {
        DatabaseService.getInstance().deleteGroupMessage(currentGroup.getId(), message.getMessageId(), new DatabaseService.DatabaseCallback<>() {
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

    private void showFullImageDialog(String userId) {
        if (userId == null) return;

        DatabaseService.getInstance().getUser(userId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(User user) {
                if (user != null && user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                    Dialog dialog = new Dialog(GroupChatActivity.this);
                    dialog.setContentView(R.layout.dialog_full_image);
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }

                    ImageView dialogImage = dialog.findViewById(R.id.dialogImage);
                    Bitmap bmp = ImageUtil.convertFrom64base(user.getProfileImage());
                    if (bmp != null) {
                        dialogImage.setImageBitmap(bmp);
                    } else {
                        dialogImage.setImageResource(R.drawable.ic_user);
                    }

                    View btnClose = dialog.findViewById(R.id.card_close_full_image);
                    if (btnClose != null) {
                        btnClose.setOnClickListener(v -> dialog.dismiss());
                    }

                    dialog.show();
                } else {
                    Toast.makeText(GroupChatActivity.this, "No profile image to display", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupChatActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        });
    }
}