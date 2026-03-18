package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.fitlink.R;
import com.example.fitlink.adapters.EventAdapter;
import com.example.fitlink.models.Event;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MyCalendarActivity extends BaseActivity {

    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private MaterialCardView layoutEmpty;
    private ProgressBar progressBar;
    private TextView tvSelectedDateTitle;
    private ChipGroup chipGroupFilter;

    private EventAdapter eventAdapter;

    // התיקון: אתחול כ-null כדי למנוע סינון והעלמת ProgressBar לפני שהנתונים הגיעו
    private List<Event> allMyEvents = null;

    private final Calendar selectedCalendar = Calendar.getInstance();
    private String currentUserId;

    // Filter states
    private static final int FILTER_ALL = 0;
    private static final int FILTER_GROUPS = 1;
    private static final int FILTER_INDEPENDENT = 2;
    private int currentFilter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_calendar);

        currentUserId = SharedPreferencesUtil.getUserId(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupCalendar();
        setupFilters();

        loadMyEvents();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_my_calendar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        calendarView = findViewById(R.id.calendarView_my_events);
        rvEvents = findViewById(R.id.rv_my_calendar_events);
        layoutEmpty = findViewById(R.id.layout_my_calendar_empty);
        progressBar = findViewById(R.id.progressBar_my_calendar);
        tvSelectedDateTitle = findViewById(R.id.tv_my_selected_date_title);
        chipGroupFilter = findViewById(R.id.chipGroup_calendar_filter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_my_calendar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(new ArrayList<>(), currentUserId, event -> {
            Intent intent = new Intent(MyCalendarActivity.this, EventDetailsActivity.class);
            // הקפדה על שליחת ה-ID בלבד
            intent.putExtra("EVENT_ID", event.getId());
            startActivity(intent);
        });
        eventAdapter.setShowGroupContext(true);
        rvEvents.setAdapter(eventAdapter);
    }

    private void setupCalendar() {
        resetTime(selectedCalendar);
        updateDateTitle();

        calendarView.setOnDayClickListener(eventDay -> {
            selectedCalendar.setTimeInMillis(eventDay.getCalendar().getTimeInMillis());
            resetTime(selectedCalendar);
            updateDateTitle();
            filterEvents();
        });
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if (id == R.id.chip_filter_groups) {
                currentFilter = FILTER_GROUPS;
            } else if (id == R.id.chip_filter_independent) {
                currentFilter = FILTER_INDEPENDENT;
            } else {
                currentFilter = FILTER_ALL;
            }

            filterEvents();
        });
    }

    private void updateDateTitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvSelectedDateTitle.setText("My Schedule - " + sdf.format(selectedCalendar.getTime()));
    }

    private void loadMyEvents() {
        progressBar.setVisibility(View.VISIBLE);

        databaseService.getAllEvents(new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                // התיקון: הסרנו את הסתרת ה-ProgressBar מכאן
                allMyEvents = new ArrayList<>();

                if (events != null) {
                    for (Event event : events) {
                        if (event.getParticipants() != null && event.getParticipants().containsKey(currentUserId)) {
                            allMyEvents.add(event);
                        }
                    }
                }

                updateCalendarEventMarkers();
                filterEvents();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MyCalendarActivity.this, "Failed to load your events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCalendarEventMarkers() {
        List<EventDay> eventsForCalendar = new ArrayList<>();

        for (Event event : allMyEvents) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(event.getStartTimestamp());
            eventsForCalendar.add(new EventDay(calendar, R.drawable.ic_event_marker));
        }

        calendarView.setEvents(eventsForCalendar);
    }

    private void filterEvents() {
        // התיקון: מונע מהפונקציה לפעול ולשנות את התצוגה לפני שהנתונים מוכנים
        if (allMyEvents == null) return;

        List<Event> filteredEvents = new ArrayList<>();
        Calendar eventCal = Calendar.getInstance();

        for (Event event : allMyEvents) {
            eventCal.setTimeInMillis(event.getStartTimestamp());
            resetTime(eventCal);

            if (eventCal.getTimeInMillis() == selectedCalendar.getTimeInMillis()) {

                boolean matchesFilter = false;
                if (currentFilter == FILTER_ALL) {
                    matchesFilter = true;
                } else if (currentFilter == FILTER_GROUPS && !event.isIndependent()) {
                    matchesFilter = true;
                } else if (currentFilter == FILTER_INDEPENDENT && event.isIndependent()) {
                    matchesFilter = true;
                }

                if (matchesFilter) {
                    filteredEvents.add(event);
                }
            }
        }

        // התיקון: מעלים את העיגול המסתובב רק כשהרשימה מוכנה
        progressBar.setVisibility(View.GONE);
        eventAdapter.updateList(filteredEvents);

        if (filteredEvents.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    private void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}