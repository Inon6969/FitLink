package com.example.fitlink.screens;

import android.content.Intent;
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
import com.example.fitlink.adapters.UserAdapter;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.User;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.List;

public class EventParticipantsActivity extends BaseActivity {

    private Event currentEvent;
    private RecyclerView rvParticipants;
    private ProgressBar progressBar;
    private LinearLayout layoutNoParticipants;
    private TextView tvParticipantsCount;

    private UserAdapter userAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_participants);

        String eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseService.getEvent(eventId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Event event) {
                if (event == null) {
                    Toast.makeText(EventParticipantsActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                currentEvent = event;
                continueInitialization();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EventParticipantsActivity.this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void continueInitialization() {
        currentUserId = SharedPreferencesUtil.getUserId(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadParticipants();
    }

    private void initViews() {
        View root = findViewById(R.id.main_event_participants);
        AppBarLayout appBarLayout = findViewById(R.id.toolbar_event_participants).getParent() instanceof AppBarLayout
                ? (AppBarLayout) findViewById(R.id.toolbar_event_participants).getParent()
                : null;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            if (appBarLayout != null) {
                appBarLayout.setPadding(0, systemBars.top, 0, 0);
            }

            return insets;
        });

        root.post(() -> ViewCompat.requestApplyInsets(root));

        rvParticipants = findViewById(R.id.rv_event_participants);
        progressBar = findViewById(R.id.progressBar_participants);
        layoutNoParticipants = findViewById(R.id.layout_no_participants);
        tvParticipantsCount = findViewById(R.id.tv_participants_count);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_event_participants);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Participants");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                Intent intent = new Intent(EventParticipantsActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }

            @Override
            public void onEditUser(User user) {
                // לא בשימוש במצב אירוע
            }

            @Override
            public void onToggleAdmin(User user) {
                // לא בשימוש במצב אירוע
            }

            @Override
            public void onDeleteUser(User user) {
                // קריאה לפונקציית הסרת משתתף
                removeParticipantFromEvent(user);
            }

            @Override
            public boolean isCurrentUser(User user) {
                return user.getId().equals(currentUserId);
            }
        });

        rvParticipants.setAdapter(userAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentEvent != null) {
            loadParticipants();
        }
    }

    private void loadParticipants() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        layoutNoParticipants.setVisibility(View.GONE);
        tvParticipantsCount.setVisibility(View.GONE);

        databaseService.getEvent(currentEvent.getId(), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Event updatedEvent) {
                if (updatedEvent != null) {
                    currentEvent = updatedEvent;
                }

                databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
                    @Override
                    public void onCompleted(List<User> allUsers) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        List<User> participantsList = new ArrayList<>();

                        if (allUsers != null) {
                            for (User user : allUsers) {
                                boolean isParticipant = currentEvent.getParticipants() != null && currentEvent.getParticipants().containsKey(user.getId());
                                boolean isCreator = currentEvent.getCreatorId() != null && currentEvent.getCreatorId().equals(user.getId());

                                if (isParticipant || isCreator) {
                                    participantsList.add(user);
                                }
                            }
                        }

                        if (participantsList.isEmpty()) {
                            layoutNoParticipants.setVisibility(View.VISIBLE);
                            rvParticipants.setVisibility(View.GONE);
                            tvParticipantsCount.setVisibility(View.GONE);
                        } else {
                            layoutNoParticipants.setVisibility(View.GONE);
                            rvParticipants.setVisibility(View.VISIBLE);

                            tvParticipantsCount.setVisibility(View.VISIBLE);
                            tvParticipantsCount.setText("Total participants: " + participantsList.size());

                            // הפעלת מצב אירוע (Event Mode) לפני העברת הרשימה לאדפטר
                            boolean isCurrentUserCreator = currentEvent.getCreatorId() != null && currentEvent.getCreatorId().equals(currentUserId);
                            userAdapter.setEventMode(true, isCurrentUserCreator, currentEvent.getCreatorId());

                            userAdapter.setUserList(participantsList);
                        }
                    }

                    @Override
                    public void onFailed(Exception e) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(EventParticipantsActivity.this, "Failed to load participants", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(EventParticipantsActivity.this, "Failed to sync event data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // הפונקציה להסרת משתתף מהאירוע באופן מיידי
    private void removeParticipantFromEvent(User user) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        databaseService.leaveEvent(currentEvent.getId(), user.getId(), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(EventParticipantsActivity.this, user.getFullName() + " was removed", Toast.LENGTH_SHORT).show();
                loadParticipants(); // טעינה מחדש של הרשימה כדי לרענן את התצוגה
            }

            @Override
            public void onFailed(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(EventParticipantsActivity.this, "Failed to remove participant", Toast.LENGTH_SHORT).show();
            }
        });
    }
}