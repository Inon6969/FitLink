package com.example.fitlink.screens.dialogs;

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
import com.example.fitlink.screens.EventsListActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateIndependentEventDialog extends Dialog {

    private final Context context;
    private final String currentUserId;

    private EditText etTitle, etDescription;
    private Spinner spinnerSport;
    private ChipGroup chipGroupLevel;
    private Button btnDate, btnTime, btnDuration, btnLocation, btnMaxParticipants, btnSave, btnCancel;
    private TextView tvSelectedLocation;
    private ProgressBar progressBar;

    private Calendar eventCalendar;
    private boolean isDateSelected = false;
    private boolean isTimeSelected = false;

    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedAddress = null;

    private long selectedDurationMillis = 0;
    private int selectedMaxParticipants = 0; // 0 מסמל "Any" (ללא הגבלה)

    // בנאי מעודכן שמקבל Context במקום Activity ספציפי
    public CreateIndependentEventDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        this.currentUserId = SharedPreferencesUtil.getUserId(context);
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

        spinnerSport = findViewById(R.id.spinner_event_sport);
        chipGroupLevel = findViewById(R.id.chipGroupEventLevel);

        btnDate = findViewById(R.id.btn_event_date);
        btnTime = findViewById(R.id.btn_event_time);
        btnDuration = findViewById(R.id.btn_event_duration);
        btnMaxParticipants = findViewById(R.id.btn_event_max_participants); // שימוש בכפתור במקום שדה טקסט
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
        btnDuration.setOnClickListener(v -> showDurationPicker());
        btnMaxParticipants.setOnClickListener(v -> showMaxParticipantsPicker());

        // התיקון הקריטי: מאפשר פתיחה גם מדף אירועים וגם מדף הניהול
        btnLocation.setOnClickListener(v -> {
            Intent mapIntent = new Intent(context, MapPickerActivity.class);
            if (context instanceof EventsListActivity) {
                ((EventsListActivity) context).getMapPickerLauncher().launch(mapIntent);
            } else if (context instanceof com.example.fitlink.screens.AdminEventsListActivity) {
                ((com.example.fitlink.screens.AdminEventsListActivity) context).getMapPickerLauncher().launch(mapIntent);
            } else {
                Toast.makeText(context, "Cannot open map from this screen", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> validateAndCreateEvent());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            eventCalendar.set(Calendar.YEAR, year);
            eventCalendar.set(Calendar.MONTH, month);
            eventCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            isDateSelected = true;
            btnDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(eventCalendar.getTime()));
        }, eventCalendar.get(Calendar.YEAR), eventCalendar.get(Calendar.MONTH), eventCalendar.get(Calendar.DAY_OF_MONTH));

        // חסימת בחירת תאריכים שעברו
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
        int maxAllowed = 100; // רף עליון לאירוע עצמאי. אפשר לשנות אם רוצים יותר/פחות

        NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(maxAllowed);
        numberPicker.setValue(selectedMaxParticipants);
        numberPicker.setWrapSelectorWheel(true);

        // הגדרת הערכים לתצוגה - הערך 0 יופיע כ-"Any"
        String[] displayedValues = new String[maxAllowed + 1];
        displayedValues[0] = "Any";
        for (int i = 1; i <= maxAllowed; i++) {
            displayedValues[i] = String.valueOf(i);
        }
        numberPicker.setDisplayedValues(displayedValues);

        // יצירת מעטפת לעיצוב יפה (מרווחים מהקצוות)
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        params.setMargins(0, 50, 0, 50);
        numberPicker.setLayoutParams(params);
        container.addView(numberPicker);

        // פתיחת דיאלוג הבחירה
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

    private void validateAndCreateEvent() {
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

        // מוודא שהאירוע העצמאי נקבע לזמן שהוא בעתיד
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

        SportType selectedSport = SportType.values()[spinnerSport.getSelectedItemPosition()];

        DifficultyLevel selectedLevel = DifficultyLevel.BEGINNER;
        int checkedId = chipGroupLevel.getCheckedChipId();
        if (checkedId == R.id.chipEventIntermediate) {
            selectedLevel = DifficultyLevel.INTERMEDIATE;
        } else if (checkedId == R.id.chipEventAdvanced) {
            selectedLevel = DifficultyLevel.ADVANCED;
        }

        Location eventLocation = new Location(selectedAddress, selectedLat, selectedLng);

        Event newEvent = new Event(
                null,
                null, // groupId is null because it's an independent event
                title,
                description,
                selectedSport,
                selectedLevel,
                startTimestamp,
                selectedDurationMillis,
                eventLocation,
                currentUserId,
                selectedMaxParticipants // שימוש במשתנה של הכפתור במקום הטקסט
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