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
import com.example.fitlink.models.Group;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.appbar.AppBarLayout; // <-- הוספנו את הייבוא הזה
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class GroupCalendarActivity extends BaseActivity {

    private Group currentGroup;
    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private MaterialCardView layoutEmpty;
    private ProgressBar progressBar;
    private TextView tvSelectedDateTitle;

    private EventAdapter eventAdapter;
    private List<Event> allGroupEvents = new ArrayList<>();
    private final Calendar selectedCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_calendar);

        String groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DatabaseService.getInstance().getGroup(groupId, new DatabaseService.DatabaseCallback<Group>() {
            @Override
            public void onCompleted(Group group) {
                if (group == null) {
                    Toast.makeText(GroupCalendarActivity.this, "Group not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                currentGroup = group;
                continueInitialization();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(GroupCalendarActivity.this, "Failed to load group details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void continueInitialization() {
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupCalendar();
        loadGroupEvents();
    }

    private void initViews() {
        View root = findViewById(R.id.main_group_calendar);
        AppBarLayout appBarLayout = findViewById(R.id.toolbar_group_calendar).getParent() instanceof AppBarLayout
                ? (AppBarLayout) findViewById(R.id.toolbar_group_calendar).getParent()
                : null;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // מרווח צדדים ותחתון למסך הראשי
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            // מרווח עליון ל-AppBarLayout כדי להרחיק את סרגל הכלים מהסוללה והשעון
            if (appBarLayout != null) {
                appBarLayout.setPadding(0, systemBars.top, 0, 0);
            }

            return insets;
        });

        // מכריח חישוב מיידי של הריווחים
        root.post(() -> ViewCompat.requestApplyInsets(root));

        calendarView = findViewById(R.id.calendarView_events);
        rvEvents = findViewById(R.id.rv_calendar_events);
        layoutEmpty = findViewById(R.id.layout_calendar_empty);
        progressBar = findViewById(R.id.progressBar_calendar);
        tvSelectedDateTitle = findViewById(R.id.tv_selected_date_title);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_group_calendar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentGroup.getName() + " Calendar");
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        String currentUserId = SharedPreferencesUtil.getUserId(this);

        eventAdapter = new EventAdapter(new ArrayList<>(), currentUserId, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Intent intent = new Intent(GroupCalendarActivity.this, EventDetailsActivity.class);
                intent.putExtra("EVENT_ID", event.getId());
                startActivity(intent);
            }
        });

        rvEvents.setAdapter(eventAdapter);
    }

    private void setupCalendar() {
        resetTime(selectedCalendar);
        updateDateTitle();

        calendarView.setOnDayClickListener(eventDay -> {
            selectedCalendar.setTimeInMillis(eventDay.getCalendar().getTimeInMillis());
            resetTime(selectedCalendar);
            updateDateTitle();
            filterEventsBySelectedDate();
        });
    }

    private void updateDateTitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvSelectedDateTitle.setText("Events on " + sdf.format(selectedCalendar.getTime()) + ":");
    }

    private void loadGroupEvents() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getEventsByGroupId(currentGroup.getId(), new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                progressBar.setVisibility(View.GONE);
                allGroupEvents = events != null ? events : new ArrayList<>();
                updateCalendarEventMarkers();
                filterEventsBySelectedDate();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupCalendarActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCalendarEventMarkers() {
        List<EventDay> eventsForCalendar = new ArrayList<>();

        for (Event event : allGroupEvents) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(event.getStartTimestamp());
            eventsForCalendar.add(new EventDay(calendar, R.drawable.ic_event_marker));
        }

        calendarView.setEvents(eventsForCalendar);
    }

    private void filterEventsBySelectedDate() {
        List<Event> filteredEvents = new ArrayList<>();
        Calendar eventCal = Calendar.getInstance();

        for (Event event : allGroupEvents) {
            eventCal.setTimeInMillis(event.getStartTimestamp());
            resetTime(eventCal);

            if (eventCal.getTimeInMillis() == selectedCalendar.getTimeInMillis()) {
                filteredEvents.add(event);
            }
        }

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