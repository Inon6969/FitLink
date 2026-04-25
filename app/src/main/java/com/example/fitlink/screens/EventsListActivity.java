package com.example.fitlink.screens;

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
import com.example.fitlink.dialogs.CreateIndependentEventDialog;
import com.example.fitlink.dialogs.EventFilterDialog;
import com.example.fitlink.enums.DifficultyLevel;
import com.example.fitlink.enums.SportType;
import com.example.fitlink.models.Event;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventsListActivity extends BaseActivity {

    private EventAdapter eventAdapter;
    private TextView tvEventCount;
    private EditText etSearch;
    private MaterialButton btnFilterEvents, btnCreateEvent;
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
    // משתני מצב לשמירת הסינון הנוכחי (כמו בקבוצות)
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
        setContentView(R.layout.activity_events_list);

        currentUserId = SharedPreferencesUtil.getUserId(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchLogic();
        setupCreateEventButton();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_events_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvEventCount = findViewById(R.id.tv_event_count);
        etSearch = findViewById(R.id.edit_EventsList_search);
        btnFilterEvents = findViewById(R.id.btn_filter_events);
        progressBar = findViewById(R.id.events_progress_bar);
        emptyState = findViewById(R.id.events_empty_state);
        btnCreateEvent = findViewById(R.id.btn_EventsList_create_event);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_events);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        RecyclerView rvEvents = findViewById(R.id.rv_events_list);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(new ArrayList<>(), currentUserId, event -> {
            Intent intent = new Intent(EventsListActivity.this, EventDetailsActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            startActivity(intent);
        });
        rvEvents.setAdapter(eventAdapter);
    }

    private void setupSearchLogic() {
        // חיפוש טקסטואלי מהיר בשורת החיפוש הכללית (לפי שם אירוע)
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

        // פתיחת חלון הסינון המתקדם
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
            boolean matchesDate = activeStartDate == null || event.getStartTimestamp() >= activeStartDate;
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
                loadIndependentEvents();
                currentCreateEventDialog = null;
            });
            currentCreateEventDialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIndependentEvents();
    }

    private void loadIndependentEvents() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getAllIndependentEvents(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<Event> events) {
                long currentTime = System.currentTimeMillis();
                allEvents = new ArrayList<>();
                if (events != null) {
                    for (Event event : events) {
                        if (event.getEndTimestamp() >= currentTime) {
                            allEvents.add(event);
                        }
                    }
                }
                executeSearch();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EventsListActivity.this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListDisplay(List<Event> listToDisplay) {
        progressBar.setVisibility(View.GONE);
        if (eventAdapter != null) eventAdapter.updateList(listToDisplay);
        tvEventCount.setText(MessageFormat.format("Showing {0} events", listToDisplay.size()));
        emptyState.setVisibility(listToDisplay.isEmpty() ? View.VISIBLE : View.GONE);
    }
}