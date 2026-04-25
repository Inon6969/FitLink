package com.example.fitlink.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
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
import com.example.fitlink.screens.EventDetailsActivity;
import com.example.fitlink.screens.EventsListActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditIndependentEventDialog extends Dialog {

    private final Context context;
    private final Event currentEvent;
    private final OnEventUpdatedListener listener;
    private final Calendar eventCalendar = Calendar.getInstance();
    private EditText etTitle, etDescription;
    private Spinner spinnerSport;
    private ChipGroup chipGroupLevel;
    private Button btnDate, btnTime, btnDuration, btnLocation, btnMaxParticipants, btnSave, btnCancel;
    private TextView tvSelectedLocation;
    private ProgressBar progressBar;
    private boolean isDateSelected = true; // נחשב true כי אנחנו שואבים נתונים קיימים
    private boolean isTimeSelected = true;

    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedAddress = null;
    private long selectedDurationMillis = 0;
    private int selectedMaxParticipants = 0; // משתנה לשמירת כמות המשתתפים

    public EditIndependentEventDialog(@NonNull Context context, Event currentEvent, OnEventUpdatedListener listener) {
        super(context);
        this.context = context;
        this.currentEvent = currentEvent;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_edit_independent_event);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        initViews();
        setupSpinners();
        prefillData();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_edit_event_title);
        etDescription = findViewById(R.id.et_edit_event_description);
        spinnerSport = findViewById(R.id.spinner_edit_event_sport);
        chipGroupLevel = findViewById(R.id.chipGroupEditEventLevel);

        btnDate = findViewById(R.id.btn_edit_event_date);
        btnTime = findViewById(R.id.btn_edit_event_time);
        btnDuration = findViewById(R.id.btn_edit_event_duration);
        btnMaxParticipants = findViewById(R.id.btn_edit_event_max_participants);
        btnLocation = findViewById(R.id.btn_edit_event_location);

        tvSelectedLocation = findViewById(R.id.tv_edit_event_selected_location);
        btnSave = findViewById(R.id.btn_edit_event_save);
        btnCancel = findViewById(R.id.btn_edit_event_cancel);
        progressBar = findViewById(R.id.progressBar_edit_event);
    }

    private void setupSpinners() {
        SportType[] sports = SportType.values();
        String[] sportNames = new String[sports.length];
        for (int i = 0; i < sports.length; i++) sportNames[i] = sports[i].getDisplayName();
        ArrayAdapter<String> sportAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sportNames);
        spinnerSport.setAdapter(sportAdapter);
    }

    private void prefillData() {
        etTitle.setText(currentEvent.getTitle());
        etDescription.setText(currentEvent.getDescription() != null ? currentEvent.getDescription() : "");

        // טעינת כמות משתתפים קיימת למשתנה ולכפתור
        selectedMaxParticipants = currentEvent.getMaxParticipants();
        if (selectedMaxParticipants == 0) {
            btnMaxParticipants.setText("Participants: Any");
        } else {
            btnMaxParticipants.setText("Participants: " + selectedMaxParticipants);
        }

        // בחירת סוג ספורט קיים
        if (currentEvent.getSportType() != null) {
            SportType[] sports = SportType.values();
            for (int i = 0; i < sports.length; i++) {
                if (sports[i] == currentEvent.getSportType()) {
                    spinnerSport.setSelection(i);
                    break;
                }
            }
        }

        // בחירת רמת קושי
        if (currentEvent.getLevel() != null) {
            if (currentEvent.getLevel() == DifficultyLevel.BEGINNER) {
                chipGroupLevel.check(R.id.chipEditEventBeginner);
            } else if (currentEvent.getLevel() == DifficultyLevel.INTERMEDIATE) {
                chipGroupLevel.check(R.id.chipEditEventIntermediate);
            } else if (currentEvent.getLevel() == DifficultyLevel.ADVANCED) {
                chipGroupLevel.check(R.id.chipEditEventAdvanced);
            }
        }

        // הגדרת זמן תאריך ושעה
        if (currentEvent.getStartTimestamp() > 0) {
            eventCalendar.setTimeInMillis(currentEvent.getStartTimestamp());
            btnDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(eventCalendar.getTime()));
            btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", eventCalendar.get(Calendar.HOUR_OF_DAY), eventCalendar.get(Calendar.MINUTE)));
        }

        // הגדרת משך זמן
        selectedDurationMillis = currentEvent.getDurationMillis();
        if (selectedDurationMillis > 0) {
            int hours = (int) (selectedDurationMillis / (1000 * 60 * 60));
            int minutes = (int) ((selectedDurationMillis / (1000 * 60)) % 60);
            if (hours > 0 && minutes > 0) btnDuration.setText(hours + "h " + minutes + "m");
            else if (hours > 0) btnDuration.setText(hours + "h");
            else btnDuration.setText(minutes + "m");
        }

        // הגדרת מיקום
        if (currentEvent.getLocation() != null && currentEvent.getLocation().getAddress() != null) {
            selectedAddress = currentEvent.getLocation().getAddress();
            selectedLat = currentEvent.getLocation().getLatitude();
            selectedLng = currentEvent.getLocation().getLongitude();
            tvSelectedLocation.setText(selectedAddress);
        }
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        btnDate.setOnClickListener(v -> showDatePicker());
        btnTime.setOnClickListener(v -> showTimePicker());
        btnDuration.setOnClickListener(v -> showDurationPicker());
        btnMaxParticipants.setOnClickListener(v -> showMaxParticipantsPicker());

        btnLocation.setOnClickListener(v -> {
            Intent mapIntent = new Intent(context, MapPickerActivity.class);
            if (context instanceof EventDetailsActivity) {
                ((EventDetailsActivity) context).getMapPickerLauncher().launch(mapIntent);
            } else if (context instanceof EventsListActivity) {
                ((EventsListActivity) context).getMapPickerLauncher().launch(mapIntent);
            } else {
                Toast.makeText(context, "Cannot open map from this screen", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> validateAndSaveChanges());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            eventCalendar.set(Calendar.YEAR, year);
            eventCalendar.set(Calendar.MONTH, month);
            eventCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            isDateSelected = true;
            btnDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(eventCalendar.getTime()));
        }, eventCalendar.get(Calendar.YEAR), eventCalendar.get(Calendar.MONTH), eventCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
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
        int currentHours = (int) (selectedDurationMillis / (1000 * 60 * 60));
        int currentMinutes = (int) ((selectedDurationMillis / (1000 * 60)) % 60);

        if (selectedDurationMillis == 0) {
            currentHours = 1;
            currentMinutes = 0;
        }

        TimePickerDialog durationPicker = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            selectedDurationMillis = (hourOfDay * 60L + minute) * 60L * 1000L;

            String formattedDuration;
            if (hourOfDay > 0 && minute > 0) {
                formattedDuration = hourOfDay + "h " + minute + "m";
            } else if (hourOfDay > 0) {
                formattedDuration = hourOfDay + "h";
            } else {
                formattedDuration = minute + "m";
            }

            if (selectedDurationMillis == 0) {
                btnDuration.setText("Duration");
                Toast.makeText(getContext(), "Duration cannot be zero", Toast.LENGTH_SHORT).show();
            } else {
                btnDuration.setText(formattedDuration);
            }

        }, currentHours, currentMinutes, true);

        durationPicker.setTitle("Select Duration");
        durationPicker.show();
    }

    private void showMaxParticipantsPicker() {
        int maxAllowed = 100; // רף עליון לאירוע עצמאי

        NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(maxAllowed);
        numberPicker.setValue(selectedMaxParticipants);
        numberPicker.setWrapSelectorWheel(true);

        String[] displayedValues = new String[maxAllowed + 1];
        displayedValues[0] = "Any";
        for (int i = 1; i <= maxAllowed; i++) {
            displayedValues[i] = String.valueOf(i);
        }
        numberPicker.setDisplayedValues(displayedValues);

        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        params.setMargins(0, 50, 0, 50);
        numberPicker.setLayoutParams(params);
        container.addView(numberPicker);

        new AlertDialog.Builder(context)
                .setTitle("Max Participants")
                .setView(container)
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedMaxParticipants = numberPicker.getValue();
                    if (selectedMaxParticipants == 0) {
                        btnMaxParticipants.setText("Participants: Any");
                    } else {
                        btnMaxParticipants.setText("Participants: " + selectedMaxParticipants);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void updateLocationDetails(String address, double lat, double lng) {
        this.selectedAddress = address;
        this.selectedLat = lat;
        this.selectedLng = lng;
        tvSelectedLocation.setText(address);
    }

    private void validateAndSaveChanges() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }
        if (!isDateSelected || !isTimeSelected) {
            Toast.makeText(getContext(), "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        long startTimestamp = eventCalendar.getTimeInMillis();
        if (startTimestamp <= System.currentTimeMillis()) {
            Toast.makeText(getContext(), "Event time must be in the future", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedDurationMillis <= 0) {
            Toast.makeText(getContext(), "Please select the event duration", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAddress == null) {
            Toast.makeText(getContext(), "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה שמונעת הקטנת כמות משתתפים מתחת לכמות המשתתפים הרשומה כרגע
        if (selectedMaxParticipants > 0 && selectedMaxParticipants < currentEvent.getParticipantsCount()) {
            Toast.makeText(getContext(),
                    "Cannot set max participants below current registered (" + currentEvent.getParticipantsCount() + ")",
                    Toast.LENGTH_LONG).show();
            return; // עוצר את השמירה
        }

        SportType selectedSport = SportType.values()[spinnerSport.getSelectedItemPosition()];

        DifficultyLevel selectedLevel = DifficultyLevel.BEGINNER;
        int checkedId = chipGroupLevel.getCheckedChipId();
        if (checkedId == R.id.chipEditEventIntermediate) {
            selectedLevel = DifficultyLevel.INTERMEDIATE;
        } else if (checkedId == R.id.chipEditEventAdvanced) {
            selectedLevel = DifficultyLevel.ADVANCED;
        }

        Location eventLocation = new Location(selectedAddress, selectedLat, selectedLng);

        // עדכון האובייקט הקיים (שימוש ב-selectedMaxParticipants במקום בטקסט)
        currentEvent.setTitle(title);
        currentEvent.setDescription(description);
        currentEvent.setSportType(selectedSport);
        currentEvent.setLevel(selectedLevel);
        currentEvent.setStartTimestamp(startTimestamp);
        currentEvent.setDurationMillis(selectedDurationMillis);
        currentEvent.setLocation(eventLocation);
        currentEvent.setMaxParticipants(selectedMaxParticipants);

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        DatabaseService.getInstance().updateEvent(currentEvent, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Event Updated!", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onEventUpdated(currentEvent);
                }
                dismiss();
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(getContext(), "Failed to update event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface OnEventUpdatedListener {
        void onEventUpdated(Event updatedEvent);
    }
}