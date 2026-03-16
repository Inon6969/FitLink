package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitlink.R;
import com.example.fitlink.adapters.CommentAdapter;
import com.example.fitlink.models.Comment;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.SportType;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.DeleteEventDialog;
import com.example.fitlink.screens.dialogs.EditEventDialog;
import com.example.fitlink.screens.dialogs.EditIndependentEventDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDetailsActivity extends BaseActivity {

    private Event currentEvent;
    private String currentUserId;
    private boolean isAdminMode = false;

    private ImageView imgIcon;
    private TextView tvTitle, tvCreator, tvDateTime, tvLocation, tvParticipants, tvDescription;
    private MaterialButton btnMainAction, btnSecondaryAction, btnTertiaryAction;

    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private EditText etNewComment;
    private MaterialButton btnSendComment;

    private EditIndependentEventDialog currentEditIndependentDialog;
    private EditEventDialog currentEditGroupEventDialog;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);

                    if (currentEditIndependentDialog != null && currentEditIndependentDialog.isShowing()) {
                        currentEditIndependentDialog.updateLocationDetails(address, lat, lng);
                    } else if (currentEditGroupEventDialog != null && currentEditGroupEventDialog.isShowing()) {
                        currentEditGroupEventDialog.updateLocationDetails(address, lat, lng);
                    }
                }
            }
    );

    public ActivityResultLauncher<Intent> getMapPickerLauncher() {
        return mapPickerLauncher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);

        String eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseService.getEvent(eventId, new DatabaseService.DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event == null) {
                    Toast.makeText(EventDetailsActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                currentEvent = event;
                continueInitialization();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EventDetailsActivity.this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void continueInitialization() {
        currentUserId = SharedPreferencesUtil.getUserId(this);
        isAdminMode = getIntent().getBooleanExtra("IS_ADMIN_MODE", false); // זיהוי אם מנהל

        initViews();
        setupToolbar();
        populateEventData();
        setupActionButtons();

        setupComments();
        loadComments();
    }

    private void initViews() {
        View root = findViewById(R.id.main_event_details);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemAndImeBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemAndImeBars.left, systemAndImeBars.top, systemAndImeBars.right, systemAndImeBars.bottom);
            return insets;
        });

        root.post(() -> ViewCompat.requestApplyInsets(root));

        imgIcon = findViewById(R.id.img_item_event_icon);
        tvTitle = findViewById(R.id.tv_details_event_title);
        tvCreator = findViewById(R.id.tv_details_event_creator);
        tvDateTime = findViewById(R.id.tv_details_event_datetime);
        tvLocation = findViewById(R.id.tv_details_event_location);
        tvParticipants = findViewById(R.id.tv_details_event_participants);
        tvDescription = findViewById(R.id.tv_details_event_description);

        btnMainAction = findViewById(R.id.btn_event_action_main);
        btnSecondaryAction = findViewById(R.id.btn_event_action_secondary);
        btnTertiaryAction = findViewById(R.id.btn_event_action_tertiary);

        rvComments = findViewById(R.id.rv_event_comments);
        etNewComment = findViewById(R.id.et_new_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);

        LinearLayout layoutDatetime = findViewById(R.id.layout_datetime_click_area);
        LinearLayout layoutLocation = findViewById(R.id.layout_location_click_area);
        LinearLayout layoutParticipants = findViewById(R.id.layout_participants_click_area);

        // בודקים אם האירוע נמצא בעבר
        boolean isPastEvent = currentEvent.getEndTimestamp() < System.currentTimeMillis();

        // המשתתפים תמיד לחיצים (כדי לראות מי היה באירוע)
        tvParticipants.setPaintFlags(tvParticipants.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        layoutParticipants.setOnClickListener(v -> {
            if (currentEvent != null) {
                Intent intent = new Intent(EventDetailsActivity.this, EventParticipantsActivity.class);
                intent.putExtra("EVENT_ID", currentEvent.getId());
                startActivity(intent);
            }
        });

        if (!isPastEvent) {
            // === אירוע עתידי: הכל פתוח ולחיץ ===
            tvDateTime.setPaintFlags(tvDateTime.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            tvLocation.setPaintFlags(tvLocation.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

            layoutLocation.setOnClickListener(v -> {
                if (currentEvent != null && currentEvent.getLocation() != null) {
                    String address = currentEvent.getLocation().getAddress();
                    double lat = currentEvent.getLocation().getLatitude();
                    double lng = currentEvent.getLocation().getLongitude();

                    android.net.Uri locationUri = android.net.Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + android.net.Uri.encode(address) + ")");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, locationUri);

                    Intent chooser = Intent.createChooser(mapIntent, "Navigate with...");
                    try {
                        startActivity(chooser);
                    } catch (Exception e) {
                        Toast.makeText(EventDetailsActivity.this, "No navigation app found", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            layoutDatetime.setOnClickListener(v -> {
                if (currentEvent != null && currentEvent.getStartTimestamp() > 0) {
                    Intent calendarIntent = new Intent(Intent.ACTION_INSERT);
                    calendarIntent.setType("vnd.android.cursor.item/event");
                    calendarIntent.putExtra(android.provider.CalendarContract.Events.TITLE, currentEvent.getTitle());
                    calendarIntent.putExtra(android.provider.CalendarContract.Events.DESCRIPTION, currentEvent.getDescription());
                    calendarIntent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, currentEvent.getStartTimestamp());
                    calendarIntent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, currentEvent.getStartTimestamp() + currentEvent.getDurationMillis());

                    if (currentEvent.getLocation() != null && currentEvent.getLocation().getAddress() != null) {
                        calendarIntent.putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, currentEvent.getLocation().getAddress());
                    }

                    try {
                        startActivity(calendarIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No calendar app found", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // === אירוע עבר: מבטלים פעולות לא הגיוניות ===
            // מורידים את הקו התחתון כדי שלא ייראה כמו קישור לחיץ
            tvDateTime.setPaintFlags(tvDateTime.getPaintFlags() & (~android.graphics.Paint.UNDERLINE_TEXT_FLAG));
            tvLocation.setPaintFlags(tvLocation.getPaintFlags() & (~android.graphics.Paint.UNDERLINE_TEXT_FLAG));

            // מקפיצים הודעה למקרה שהמשתמש בכל זאת מנסה ללחוץ
            layoutLocation.setOnClickListener(v -> Toast.makeText(this, "Cannot navigate to a past event.", Toast.LENGTH_SHORT).show());
            layoutDatetime.setOnClickListener(v -> Toast.makeText(this, "Event has already ended.", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_event_details);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Details");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void populateEventData() {
        tvTitle.setText(currentEvent.getTitle());

        if (currentEvent.getSportType() != null) {
            imgIcon.setImageResource(getSportIconResource(currentEvent.getSportType()));
        }

        if (currentEvent.getStartTimestamp() > 0) {
            String formattedDate = dateFormat.format(new Date(currentEvent.getStartTimestamp()));
            tvDateTime.setText(formattedDate + " (" + currentEvent.getFormattedDuration() + ")");
        } else {
            tvDateTime.setText("Time not set");
        }

        if (currentEvent.getLocation() != null && currentEvent.getLocation().getAddress() != null) {
            tvLocation.setText(currentEvent.getLocation().getAddress());
        } else {
            tvLocation.setText("No location");
        }

        int participants = currentEvent.getParticipantsCount();
        String limit = currentEvent.getMaxParticipants() > 0 ? String.valueOf(currentEvent.getMaxParticipants()) : "Unlimited";
        tvParticipants.setText(participants + "/" + limit + " Participants");

        String description = currentEvent.getDescription();
        tvDescription.setText((description == null || description.isEmpty()) ? "No description provided." : description);

        tvCreator.setText("Loading creator...");
        if (currentEvent.getCreatorId() != null) {
            databaseService.getUser(currentEvent.getCreatorId(), new DatabaseService.DatabaseCallback<User>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null) {
                        tvCreator.setText(String.format("By %s %s", user.getFirstName(), user.getLastName()));
                    } else {
                        tvCreator.setText("By Unknown");
                    }
                }
                @Override
                public void onFailed(Exception e) {
                    tvCreator.setText("By Unknown");
                }
            });
        }
    }

    private void setupActionButtons() {
        boolean isCreator = currentEvent.getCreatorId() != null && currentEvent.getCreatorId().equals(currentUserId);
        boolean isJoined = currentEvent.getParticipants() != null && currentEvent.getParticipants().containsKey(currentUserId);
        boolean isIndependent = currentEvent.isIndependent();

        // המנהל מקבל הרשאות ניהול בדיוק כמו יוצר האירוע!
        boolean canManage = isCreator || isAdminMode;

        boolean isPastEvent = currentEvent.getEndTimestamp() < System.currentTimeMillis();

        // איפוס מצב התחלתי
        btnMainAction.setVisibility(View.VISIBLE);
        btnSecondaryAction.setVisibility(View.GONE);
        btnTertiaryAction.setVisibility(View.GONE);

        // 1. טיפול באירוע עבר
        if (isPastEvent) {
            btnMainAction.setText("Event Completed");
            btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
            btnMainAction.setEnabled(false);

            if (canManage) {
                // מחיקה תמיד בכפתור השלישי
                btnTertiaryAction.setVisibility(View.VISIBLE);
                btnTertiaryAction.setText("Delete Event");
                btnTertiaryAction.setOnClickListener(v -> deleteEvent());
            }
            return;
        }

        btnMainAction.setEnabled(true);

        // 2. טיפול בכפתור הראשי (הצטרפות / עזיבה / הסתרה)
        if (isIndependent && isCreator) {
            // יוצר של אירוע עצמאי לא צריך כפתור הצטרפות/עזיבה, לכן נסתיר אותו
            btnMainAction.setVisibility(View.GONE);
        } else {
            // כל השאר מקבלים אפשרות הצטרפות או עזיבה בכפתור הראשי
            btnMainAction.setVisibility(View.VISIBLE);
            if (isJoined) {
                btnMainAction.setText("Leave Event");
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                btnMainAction.setOnClickListener(v -> leaveEvent());
            } else {
                btnMainAction.setText("Join Event");
                btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.fitlinkPrimary)));
                btnMainAction.setOnClickListener(v -> joinEvent());
            }
        }

        // 3. טיפול בכפתורי הניהול (תמיד יופיעו באותו עיצוב למנהלים וליוצרים)
        if (canManage) {
            // עריכה תמיד בכפתור המשני (Outlined)
            btnSecondaryAction.setVisibility(View.VISIBLE);
            btnSecondaryAction.setText("Edit Event");
            btnSecondaryAction.setOnClickListener(v -> editEvent());

            // מחיקה תמיד בכפתור השלישי
            btnTertiaryAction.setVisibility(View.VISIBLE);
            btnTertiaryAction.setText("Delete Event");
            btnTertiaryAction.setOnClickListener(v -> deleteEvent());
        }
    }

    private void joinEvent() {
        if (currentEvent.getMaxParticipants() > 0 && currentEvent.getParticipantsCount() >= currentEvent.getMaxParticipants()) {
            Toast.makeText(this, "Event is full", Toast.LENGTH_SHORT).show();
            return;
        }

        btnMainAction.setEnabled(false);
        databaseService.joinEvent(currentEvent.getId(), currentUserId, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                btnMainAction.setEnabled(true);
                Toast.makeText(EventDetailsActivity.this, "Joined successfully", Toast.LENGTH_SHORT).show();

                currentEvent.addParticipant(currentUserId);
                populateEventData();
                setupActionButtons();
            }

            @Override
            public void onFailed(Exception e) {
                btnMainAction.setEnabled(true);
                Toast.makeText(EventDetailsActivity.this, "Failed to join event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveEvent() {
        btnMainAction.setEnabled(false);
        databaseService.leaveEvent(currentEvent.getId(), currentUserId, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                btnMainAction.setEnabled(true);
                Toast.makeText(EventDetailsActivity.this, "Left event", Toast.LENGTH_SHORT).show();

                currentEvent.removeParticipant(currentUserId);
                populateEventData();
                setupActionButtons();
            }

            @Override
            public void onFailed(Exception e) {
                btnMainAction.setEnabled(true);
                Toast.makeText(EventDetailsActivity.this, "Failed to leave", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editEvent() {
        if (currentEvent.isIndependent()) {
            currentEditIndependentDialog = new EditIndependentEventDialog(this, currentEvent, updatedEvent -> {
                this.currentEvent = updatedEvent;
                populateEventData();
            });
            currentEditIndependentDialog.show();
        } else {
            currentEditGroupEventDialog = new EditEventDialog(this, currentEvent, updatedEvent -> {
                this.currentEvent = updatedEvent;
                populateEventData();
            });
            currentEditGroupEventDialog.show();
        }
    }

    private void deleteEvent() {
        new DeleteEventDialog(this, () -> {
            if (btnMainAction != null) btnMainAction.setEnabled(false);
            if (btnTertiaryAction != null) btnTertiaryAction.setEnabled(false);

            databaseService.deleteEvent(currentEvent.getId(), new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    Toast.makeText(EventDetailsActivity.this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailed(Exception e) {
                    if (btnMainAction != null) btnMainAction.setEnabled(true);
                    if (btnTertiaryAction != null) btnTertiaryAction.setEnabled(true);
                    Toast.makeText(EventDetailsActivity.this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                }
            });
        }).show();
    }

    private int getSportIconResource(SportType type) {
        if (type == SportType.RUNNING) return R.drawable.ic_running;
        if (type == SportType.SWIMMING) return R.drawable.ic_swimming;
        if (type == SportType.CYCLING) return R.drawable.ic_cycling;
        return R.drawable.ic_sport;
    }

    private void setupComments() {
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(new ArrayList<>());
        rvComments.setAdapter(commentAdapter);

        // חישוב מתי נועלים את התגובות (3 ימים = 3 * 24 * 60 * 60 * 1000 מילישניות)
        long THREE_DAYS_MILLIS = 3L * 24 * 60 * 60 * 1000;
        boolean isCommentsLocked = currentEvent.getEndTimestamp() + THREE_DAYS_MILLIS < System.currentTimeMillis();

        View commentBar = findViewById(R.id.layout_comment_bar);

        if (isCommentsLocked) {
            // אם עברו 3 ימים מסיום האירוע - מסתירים לגמרי את שורת הקלדת התגובה!
            if (commentBar != null) {
                commentBar.setVisibility(View.GONE);
            }
        } else {
            // אם אנחנו עדיין בטווח הזמן המותר, מאפשרים לשלוח תגובות כרגיל
            if (commentBar != null) {
                commentBar.setVisibility(View.VISIBLE);
            }

            btnSendComment.setOnClickListener(v -> {
                String text = etNewComment.getText() != null ? etNewComment.getText().toString().trim() : "";

                if (text.isEmpty()) {
                    return;
                }

                Comment newComment = new Comment(
                        "",
                        currentEvent.getId(),
                        currentUserId,
                        text,
                        System.currentTimeMillis()
                );

                btnSendComment.setEnabled(false);

                databaseService.addEventComment(newComment, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        etNewComment.setText("");
                        btnSendComment.setEnabled(true);
                        Toast.makeText(EventDetailsActivity.this, "Comment added", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        btnSendComment.setEnabled(true);
                        Toast.makeText(EventDetailsActivity.this, "Failed to send comment", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    private void loadComments() {
        databaseService.getEventComments(currentEvent.getId(), new DatabaseService.DatabaseCallback<List<Comment>>() {
            @Override
            public void onCompleted(List<Comment> comments) {
                if (commentAdapter != null) {
                    commentAdapter.updateList(comments);

                    if (!comments.isEmpty()) {
                        rvComments.scrollToPosition(comments.size() - 1);
                    }
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EventDetailsActivity.this, "Failed to load comments", Toast.LENGTH_SHORT).show();
            }
        });
    }
}