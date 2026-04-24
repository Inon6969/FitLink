package com.example.fitlink.screens;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.example.fitlink.adapters.EventAdapter;
import com.example.fitlink.models.DifficultyLevel;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.SportType;
import com.example.fitlink.screens.dialogs.CreateIndependentEventDialog;
import com.example.fitlink.screens.dialogs.EventFilterDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminEventsListActivity extends BaseActivity {

    private EventAdapter eventAdapter;
    private TextView tvEventCount;
    private EditText etSearch;
    private MaterialButton btnFilterEvents, btnCreateEvent, btnCleanupEvents;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private List<Event> allEvents = null;
    private String currentUserId;
    private CreateIndependentEventDialog currentCreateEventDialog;
    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);
                    if (currentCreateEventDialog != null && currentCreateEventDialog.isShowing()) {
                        currentCreateEventDialog.updateLocationDetails(address, lat, lng);
                    }
                }
            }
    );
    // משתני מצב לשמירת הסינון הנוכחי (כמו בשאר המסכים)
    private SportType activeSportFilter = null;
    private DifficultyLevel activeLevelFilter = null;
    private String activeLocationFilter = "";
    private Long activeStartDate = null;
    private Long activeEndDate = null;
    private Integer activeMinDuration = null;
    private Integer activeMaxDuration = null;

    public ActivityResultLauncher<Intent> getMapPickerLauncher() {
        return mapPickerLauncher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_events_list);

        currentUserId = SharedPreferencesUtil.getUserId(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchLogic();
        setupCreateEventButton();
        setupCleanupButton();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_admin_events_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvEventCount = findViewById(R.id.tv_admin_event_count);
        etSearch = findViewById(R.id.edit_admin_events_search);
        btnFilterEvents = findViewById(R.id.btn_filter_admin_events);
        progressBar = findViewById(R.id.admin_events_progress_bar);
        emptyState = findViewById(R.id.admin_events_empty_state);
        btnCreateEvent = findViewById(R.id.btn_admin_create_event);
        btnCleanupEvents = findViewById(R.id.btn_admin_cleanup_events);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_admin_events);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        RecyclerView rvEvents = findViewById(R.id.rv_admin_events_list);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(new ArrayList<>(), currentUserId, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Intent intent = new Intent(AdminEventsListActivity.this, EventDetailsActivity.class);
                intent.putExtra("EVENT_ID", event.getId());
                intent.putExtra("IS_ADMIN_MODE", true);
                startActivity(intent);
            }
        });
        eventAdapter.setShowGroupContext(true);
        rvEvents.setAdapter(eventAdapter);
    }

    private void setupSearchLogic() {
        // חיפוש טקסטואלי רגיל (לפי שם)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                executeSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // פתיחת חלון הסינון לאירועים
        btnFilterEvents.setOnClickListener(v -> {
            EventFilterDialog dialog = new EventFilterDialog(this,
                    activeSportFilter, activeLevelFilter, activeLocationFilter,
                    activeStartDate, activeEndDate, activeMinDuration, activeMaxDuration,
                    (sportType, level, location, startDate, endDate, minDur, maxDur) -> {
                        // שמירת בחירות המשתמש
                        activeSportFilter = sportType;
                        activeLevelFilter = level;
                        activeLocationFilter = (location != null) ? location : "";
                        activeStartDate = startDate;
                        activeEndDate = endDate;
                        activeMinDuration = minDur;
                        activeMaxDuration = maxDur;

                        // הפעלת סינון
                        executeSearch();
                    });
            dialog.show();
        });
    }

    private void executeSearch() {
        if (allEvents == null) return;

        String nameQuery = etSearch.getText().toString().toLowerCase().trim();

        List<Event> filteredList = allEvents.stream().filter(event -> {
            // 1. סינון לפי שם
            boolean matchesName = nameQuery.isEmpty() ||
                    (event.getTitle() != null && event.getTitle().toLowerCase().contains(nameQuery));

            // 2. סינון ספורט
            boolean matchesSport = activeSportFilter == null ||
                    (event.getSportType() != null && event.getSportType() == activeSportFilter);

            // 3. סינון רמה
            boolean matchesLevel = activeLevelFilter == null ||
                    (event.getLevel() != null && event.getLevel() == activeLevelFilter);

            // 4. סינון מיקום
            boolean matchesLocation = activeLocationFilter.isEmpty() ||
                    (event.getLocation() != null && event.getLocation().getAddress() != null &&
                            event.getLocation().getAddress().toLowerCase().contains(activeLocationFilter.toLowerCase()));

            // 5. סינון תאריכים
            boolean matchesDate = true;
            if (activeStartDate != null && event.getStartTimestamp() < activeStartDate)
                matchesDate = false;
            if (activeEndDate != null && event.getStartTimestamp() > activeEndDate)
                matchesDate = false;

            // 6. סינון משך זמן
            boolean matchesDuration = true;
            if (activeMinDuration != null || activeMaxDuration != null) {
                long durationMins = event.getDurationMillis() / (60 * 1000L);
                if (activeMinDuration != null && durationMins < activeMinDuration)
                    matchesDuration = false;
                if (activeMaxDuration != null && durationMins > activeMaxDuration)
                    matchesDuration = false;
            }

            // אירוע יוצג רק אם עמד בכל הקריטריונים (AND)
            return matchesName && matchesSport && matchesLevel && matchesLocation && matchesDate && matchesDuration;

        }).collect(Collectors.toList());

        updateListDisplay(filteredList);
    }

    private void setupCreateEventButton() {
        btnCreateEvent.setOnClickListener(v -> {
            currentCreateEventDialog = new CreateIndependentEventDialog(this);
            currentCreateEventDialog.setOnDismissListener(d -> {
                loadAllEvents();
                currentCreateEventDialog = null;
            });
            currentCreateEventDialog.show();
        });
    }

    // --- מערכת הניקוי המשודרגת עם בחירת תאריך ושעה ---
    private void setupCleanupButton() {
        btnCleanupEvents.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                // לאחר בחירת התאריך, מקפיץ את בחירת השעה
                TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                    // חיבור התאריך והשעה יחד לנקודת חיתוך מדויקת
                    Calendar selectedDateTime = Calendar.getInstance();
                    selectedDateTime.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                    long cutoffTimestamp = selectedDateTime.getTimeInMillis();

                    String formattedDateTime = String.format(Locale.getDefault(), "%02d/%02d/%d %02d:%02d", dayOfMonth, month + 1, year, hourOfDay, minute);
                    showCleanupConfirmation(cutoffTimestamp, formattedDateTime);

                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);

                timePickerDialog.setTitle("Select Time");
                timePickerDialog.show();

            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

            // מונע בחירת תאריכים בעתיד
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.setTitle("Select Date");
            datePickerDialog.show();
        });
    }

    private void showCleanupConfirmation(long cutoffTimestamp, String formattedDate) {
        String title = "Confirm Cleanup";
        String message = "Are you sure you want to permanently delete all events that ended before " + formattedDate + "?\n\n(Gamification stats for users will be safely preserved).";

        new com.example.fitlink.screens.dialogs.DeleteEventDialog(this, title, message, () -> {
            progressBar.setVisibility(View.VISIBLE);
            btnCleanupEvents.setEnabled(false);
            btnCleanupEvents.setText("Cleaning...");

            databaseService.cleanupOldEvents(cutoffTimestamp, new DatabaseService.DatabaseCallback<Integer>() {
                @Override
                public void onCompleted(Integer deletedCount) {
                    progressBar.setVisibility(View.GONE);
                    btnCleanupEvents.setEnabled(true);
                    btnCleanupEvents.setText("Clean Old Events");

                    if (deletedCount == 0) {
                        Toast.makeText(AdminEventsListActivity.this, "No old events found for this timeframe.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminEventsListActivity.this, "Cleanup finished! " + deletedCount + " old events removed.", Toast.LENGTH_LONG).show();
                    }

                    loadAllEvents();
                }

                @Override
                public void onFailed(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    btnCleanupEvents.setEnabled(true);
                    btnCleanupEvents.setText("Clean Old Events");
                    Toast.makeText(AdminEventsListActivity.this, "Cleanup failed.", Toast.LENGTH_SHORT).show();
                }
            });
        }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllEvents();
    }

    private void loadAllEvents() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getAllEvents(new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                allEvents = (events != null) ? events : new ArrayList<>();
                executeSearch();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminEventsListActivity.this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<Event> listToDisplay) {
        progressBar.setVisibility(View.GONE);
        if (eventAdapter != null) eventAdapter.updateList(listToDisplay);
        tvEventCount.setText(MessageFormat.format("Total events: {0}", listToDisplay.size()));
        emptyState.setVisibility(listToDisplay.isEmpty() ? View.VISIBLE : View.GONE);
    }
}