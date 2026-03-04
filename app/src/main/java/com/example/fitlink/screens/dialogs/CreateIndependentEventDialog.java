package com.example.fitlink.screens.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.fitlink.R;
import com.example.fitlink.models.DifficultyLevel;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.Location;
import com.example.fitlink.models.SportType;
import com.example.fitlink.screens.EventsListActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateIndependentEventDialog extends Dialog {

    private final EventsListActivity parentActivity;
    private final String currentUserId;

    private EditText etTitle, etDescription, etMaxParticipants;
    private Spinner spinnerSport;
    private ChipGroup chipGroupLevel;
    private Button btnDate, btnTime, btnDuration, btnLocation, btnSave, btnCancel;
    private TextView tvSelectedLocation;
    private ProgressBar progressBar;

    private Calendar eventCalendar;
    private boolean isDateSelected = false;
    private boolean isTimeSelected = false;

    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedAddress = null;

    private int selectedDuration = 60; // ברירת מחדל של 60 דקות

    public CreateIndependentEventDialog(@NonNull EventsListActivity activity) {
        super(activity);
        this.parentActivity = activity;
        this.currentUserId = SharedPreferencesUtil.getUserId(activity);
        this.eventCalendar = Calendar.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_create_independent_event);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        initViews();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_event_title);
        etDescription = findViewById(R.id.et_event_description);
        etMaxParticipants = findViewById(R.id.et_event_max_participants);
        spinnerSport = findViewById(R.id.spinner_event_sport);
        chipGroupLevel = findViewById(R.id.chipGroupEventLevel); // עדכון מ-Spinner ל-ChipGroup

        btnDate = findViewById(R.id.btn_event_date);
        btnTime = findViewById(R.id.btn_event_time);
        btnDuration = findViewById(R.id.btn_event_duration); // עדכון מ-EditText ל-Button
        btnLocation = findViewById(R.id.btn_event_location);

        tvSelectedLocation = findViewById(R.id.tv_event_selected_location);
        btnSave = findViewById(R.id.btn_event_save);
        btnCancel = findViewById(R.id.btn_event_cancel);
        progressBar = findViewById(R.id.progressBar_create_event);
    }

    private void setupSpinners() {
        SportType[] sports = SportType.values();
        String[] sportNames = new String[sports.length];
        for (int i = 0; i < sports.length; i++) sportNames[i] = sports[i].getDisplayName();
        ArrayAdapter<String> sportAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sportNames);
        spinnerSport.setAdapter(sportAdapter);
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnDate.setOnClickListener(v -> showDatePicker());
        btnTime.setOnClickListener(v -> showTimePicker());

        // הוספת חלון בחירת משך זמן
        btnDuration.setOnClickListener(v -> showDurationPicker());

        btnLocation.setOnClickListener(v -> {
            Intent mapIntent = new Intent(parentActivity, MapPickerActivity.class);
            parentActivity.getMapPickerLauncher().launch(mapIntent);
        });

        btnSave.setOnClickListener(v -> validateAndCreateEvent());
    }

    private void showDatePicker() {
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            eventCalendar.set(Calendar.YEAR, year);
            eventCalendar.set(Calendar.MONTH, month);
            eventCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            isDateSelected = true;
            btnDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(eventCalendar.getTime()));
        }, eventCalendar.get(Calendar.YEAR), eventCalendar.get(Calendar.MONTH), eventCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            eventCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            eventCalendar.set(Calendar.MINUTE, minute);
            eventCalendar.set(Calendar.SECOND, 0);
            isTimeSelected = true;
            btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
        }, eventCalendar.get(Calendar.HOUR_OF_DAY), eventCalendar.get(Calendar.MINUTE), true).show();
    }

    private void showDurationPicker() {
        String[] options = {"30 Mins", "45 Mins", "60 Mins", "90 Mins", "120 Mins"};
        int[] values = {30, 45, 60, 90, 120};

        new AlertDialog.Builder(getContext())
                .setTitle("Select Duration")
                .setItems(options, (dialog, which) -> {
                    selectedDuration = values[which];
                    btnDuration.setText(options[which]);
                }).show();
    }

    public void updateLocationDetails(String address, double lat, double lng) {
        this.selectedAddress = address;
        this.selectedLat = lat;
        this.selectedLng = lng;
        tvSelectedLocation.setText(address);
    }

    private void validateAndCreateEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String maxPplStr = etMaxParticipants.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }
        if (!isDateSelected || !isTimeSelected) {
            Toast.makeText(getContext(), "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAddress == null) {
            Toast.makeText(getContext(), "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        long durationMillis = selectedDuration * 60 * 1000L;
        int maxParticipants = TextUtils.isEmpty(maxPplStr) ? 0 : Integer.parseInt(maxPplStr);

        SportType selectedSport = SportType.values()[spinnerSport.getSelectedItemPosition()];

        // משיכת הנתונים מה-ChipGroup
        DifficultyLevel selectedLevel = DifficultyLevel.BEGINNER;
        int checkedId = chipGroupLevel.getCheckedChipId();
        if (checkedId == R.id.chipEventIntermediate) {
            selectedLevel = DifficultyLevel.INTERMEDIATE;
        } else if (checkedId == R.id.chipEventAdvanced) {
            selectedLevel = DifficultyLevel.ADVANCED;
        }

        long startTimestamp = eventCalendar.getTimeInMillis();
        Location eventLocation = new Location(selectedAddress, selectedLat, selectedLng);

        Event newEvent = new Event(
                null,
                null,
                title,
                description,
                selectedSport,
                selectedLevel,
                startTimestamp,
                durationMillis,
                eventLocation,
                currentUserId,
                maxParticipants
        );

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        DatabaseService.getInstance().createNewEvent(newEvent, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Event Created!", Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(getContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}