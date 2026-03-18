package com.example.fitlink.screens;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class EventsListActivity extends BaseActivity {

    private EventAdapter eventAdapter;
    private TextView tvEventCount;
    private EditText etSearch, etDurationMin, etDurationMax;
    private Spinner spinnerSearchType, spinnerSearchOptions;
    private TextInputLayout layoutSearchText;
    private LinearLayout layoutRangePicker, layoutDurationPicker;
    private MaterialButton btnRangeStart, btnRangeEnd;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private MaterialButton btnCreateEvent;

    // התיקון: אתחול כ-null כדי למנוע העלמה מוקדמת של ה-ProgressBar
    private List<Event> allEvents = null;

    private String currentUserId;
    private CreateIndependentEventDialog currentCreateEventDialog;

    private Long filterStartDate = null;
    private Long filterEndDate = null;
    private Integer filterStartTimeMins = null;
    private Integer filterEndTimeMins = null;

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

    public ActivityResultLauncher<Intent> getMapPickerLauncher() { return mapPickerLauncher; }

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
        spinnerSearchType = findViewById(R.id.spinner_events_search_type);
        spinnerSearchOptions = findViewById(R.id.spinner_events_search_options);
        layoutSearchText = findViewById(R.id.layout_search_text);

        layoutRangePicker = findViewById(R.id.layout_range_picker);
        btnRangeStart = findViewById(R.id.btn_range_start);
        btnRangeEnd = findViewById(R.id.btn_range_end);

        layoutDurationPicker = findViewById(R.id.layout_duration_picker);
        etDurationMin = findViewById(R.id.et_duration_min);
        etDurationMax = findViewById(R.id.et_duration_max);

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

        eventAdapter = new EventAdapter(new ArrayList<>(), currentUserId, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Intent intent = new Intent(EventsListActivity.this, EventDetailsActivity.class);
                intent.putExtra("EVENT_ID", event.getId());
                startActivity(intent);
            }
        });
        rvEvents.setAdapter(eventAdapter);
    }

    private void setupSearchLogic() {
        String[] searchOptions = {"Title", "Sport Type", "Level", "Location", "Date", "Time", "Duration"};
        spinnerSearchType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, searchOptions));

        DifficultyLevel[] levels = DifficultyLevel.values();
        String[] levelNames = new String[levels.length];
        for(int i=0; i<levels.length; i++) levelNames[i] = levels[i].getDisplayName();
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, levelNames);

        SportType[] sports = SportType.values();
        String[] sportNames = new String[sports.length];
        for(int i=0; i<sports.length; i++) sportNames[i] = sports[i].getDisplayName();
        ArrayAdapter<String> sportAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sportNames);

        spinnerSearchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = searchOptions[position];

                filterStartDate = null; filterEndDate = null;
                filterStartTimeMins = null; filterEndTimeMins = null;
                btnRangeStart.setText("Start " + selectedType);
                btnRangeEnd.setText("End " + selectedType);

                if (selectedType.equals("Level")) {
                    layoutSearchText.setVisibility(View.GONE);
                    layoutRangePicker.setVisibility(View.GONE);
                    layoutDurationPicker.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setAdapter(levelAdapter);
                } else if (selectedType.equals("Sport Type")) {
                    layoutSearchText.setVisibility(View.GONE);
                    layoutRangePicker.setVisibility(View.GONE);
                    layoutDurationPicker.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setAdapter(sportAdapter);
                } else if (selectedType.equals("Duration")) {
                    layoutSearchText.setVisibility(View.GONE);
                    layoutRangePicker.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.GONE);
                    layoutDurationPicker.setVisibility(View.VISIBLE);
                } else if (selectedType.equals("Date") || selectedType.equals("Time")) {
                    layoutSearchText.setVisibility(View.GONE);
                    spinnerSearchOptions.setVisibility(View.GONE);
                    layoutDurationPicker.setVisibility(View.GONE);
                    layoutRangePicker.setVisibility(View.VISIBLE);
                } else {
                    layoutSearchText.setVisibility(View.VISIBLE);
                    spinnerSearchOptions.setVisibility(View.GONE);
                    layoutRangePicker.setVisibility(View.GONE);
                    layoutDurationPicker.setVisibility(View.GONE);
                    layoutSearchText.setHint("Type to search...");
                }
                executeSearch();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSearchOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { executeSearch(); }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { executeSearch(); }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etSearch.addTextChangedListener(textWatcher);
        etDurationMin.addTextChangedListener(textWatcher);
        etDurationMax.addTextChangedListener(textWatcher);

        btnRangeStart.setOnClickListener(v -> showPickerForStart(spinnerSearchType.getSelectedItem().toString()));
        btnRangeEnd.setOnClickListener(v -> showPickerForEnd(spinnerSearchType.getSelectedItem().toString()));
    }

    private void showPickerForStart(String type) {
        if (type.equals("Date")) {
            Calendar c = Calendar.getInstance();
            if (filterStartDate != null) c.setTimeInMillis(filterStartDate);
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, 0, 0, 0);
                filterStartDate = selected.getTimeInMillis();
                btnRangeStart.setText(String.format("%02d/%02d/%d", day, month + 1, year));
                executeSearch();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        } else if (type.equals("Time")) {
            int h = 12, m = 0;
            if (filterStartTimeMins != null) { h = filterStartTimeMins / 60; m = filterStartTimeMins % 60; }
            new TimePickerDialog(this, (view, hour, minute) -> {
                filterStartTimeMins = hour * 60 + minute;
                btnRangeStart.setText(String.format("%02d:%02d", hour, minute));
                executeSearch();
            }, h, m, true).show();
        }
    }

    private void showPickerForEnd(String type) {
        if (type.equals("Date")) {
            Calendar c = Calendar.getInstance();
            if (filterEndDate != null) c.setTimeInMillis(filterEndDate);
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, 23, 59, 59);
                filterEndDate = selected.getTimeInMillis();
                btnRangeEnd.setText(String.format("%02d/%02d/%d", day, month + 1, year));
                executeSearch();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        } else if (type.equals("Time")) {
            int h = 12, m = 0;
            if (filterEndTimeMins != null) { h = filterEndTimeMins / 60; m = filterEndTimeMins % 60; }
            new TimePickerDialog(this, (view, hour, minute) -> {
                filterEndTimeMins = hour * 60 + minute;
                btnRangeEnd.setText(String.format("%02d:%02d", hour, minute));
                executeSearch();
            }, h, m, true).show();
        }
    }

    private void executeSearch() {
        if (allEvents == null) return;
        String searchType = spinnerSearchType.getSelectedItem() != null ? spinnerSearchType.getSelectedItem().toString() : "Title";
        List<Event> filteredList;

        if (searchType.equals("Level")) {
            if (spinnerSearchOptions.getSelectedItem() == null) return;
            String selectedLvl = spinnerSearchOptions.getSelectedItem().toString();
            filteredList = allEvents.stream()
                    .filter(e -> e.getLevel() != null && e.getLevel().getDisplayName().equals(selectedLvl))
                    .collect(Collectors.toList());
        } else if (searchType.equals("Sport Type")) {
            if (spinnerSearchOptions.getSelectedItem() == null) return;
            String selectedSport = spinnerSearchOptions.getSelectedItem().toString();
            filteredList = allEvents.stream()
                    .filter(e -> e.getSportType() != null && e.getSportType().getDisplayName().equals(selectedSport))
                    .collect(Collectors.toList());
        } else if (searchType.equals("Duration")) {
            String minStr = etDurationMin.getText().toString().trim();
            String maxStr = etDurationMax.getText().toString().trim();

            long minMins = minStr.isEmpty() ? 0 : Long.parseLong(minStr);
            long maxMins = maxStr.isEmpty() ? Long.MAX_VALUE : Long.parseLong(maxStr);

            filteredList = allEvents.stream()
                    .filter(e -> {
                        long durationInMins = e.getDurationMillis() / (60 * 1000L);
                        return durationInMins >= minMins && durationInMins <= maxMins;
                    })
                    .collect(Collectors.toList());
        } else if (searchType.equals("Date")) {
            filteredList = allEvents.stream().filter(e -> {
                long start = e.getStartTimestamp();
                boolean pass = true;
                if (filterStartDate != null) pass = pass && (start >= filterStartDate);
                if (filterEndDate != null) pass = pass && (start <= filterEndDate);
                return pass;
            }).collect(Collectors.toList());
        } else if (searchType.equals("Time")) {
            filteredList = allEvents.stream().filter(e -> {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(e.getStartTimestamp());
                int mins = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
                boolean pass = true;
                if (filterStartTimeMins != null) pass = pass && (mins >= filterStartTimeMins);
                if (filterEndTimeMins != null) pass = pass && (mins <= filterEndTimeMins);
                return pass;
            }).collect(Collectors.toList());
        } else {
            String query = etSearch.getText().toString().toLowerCase().trim();
            if (query.isEmpty()) {
                updateListDisplay(allEvents);
                return;
            }

            filteredList = allEvents.stream().filter(event -> {
                if (searchType.equals("Title")) return event.getTitle() != null && event.getTitle().toLowerCase().contains(query);
                if (searchType.equals("Location")) return event.getLocation() != null && event.getLocation().getAddress() != null && event.getLocation().getAddress().toLowerCase().contains(query);
                return false;
            }).collect(Collectors.toList());
        }
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
        databaseService.getAllIndependentEvents(new DatabaseService.DatabaseCallback<List<Event>>() {
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
        // התיקון: מוודא שה-ProgressBar נעלם רק כשהרשימה מוכנה
        progressBar.setVisibility(View.GONE);
        if (eventAdapter != null) eventAdapter.updateList(listToDisplay);
        tvEventCount.setText(MessageFormat.format("Showing {0} events", listToDisplay.size()));
        emptyState.setVisibility(listToDisplay.isEmpty() ? View.VISIBLE : View.GONE);
    }
}