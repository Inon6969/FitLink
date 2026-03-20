package com.example.fitlink.screens.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import com.example.fitlink.R;
import com.example.fitlink.models.DifficultyLevel;
import com.example.fitlink.models.SportType;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventFilterDialog extends Dialog {

    private Spinner spinnerSport, spinnerLevel;
    private EditText etLocation;
    private MaterialButton btnStartDate, btnEndDate, btnMinDur, btnMaxDur, btnApply, btnClear;
    private OnEventFilterAppliedListener listener;

    private SportType currentSport;
    private DifficultyLevel currentLevel;
    private String currentLocation;
    private Long currentStartDate, currentEndDate;
    private Integer currentMinDur, currentMaxDur;

    public interface OnEventFilterAppliedListener {
        void onFilterApplied(SportType sportType, DifficultyLevel level, String location, Long startDate, Long endDate, Integer minDuration, Integer maxDuration);
    }

    public EventFilterDialog(Context context, SportType sport, DifficultyLevel level, String location, Long startDate, Long endDate, Integer minDur, Integer maxDur, OnEventFilterAppliedListener listener) {
        super(context);
        this.currentSport = sport;
        this.currentLevel = level;
        this.currentLocation = location;
        this.currentStartDate = startDate;
        this.currentEndDate = endDate;
        this.currentMinDur = minDur;
        this.currentMaxDur = maxDur;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filter_events);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        spinnerSport = findViewById(R.id.spinner_filter_sport);
        spinnerLevel = findViewById(R.id.spinner_filter_level);
        etLocation = findViewById(R.id.edit_filter_location);

        btnStartDate = findViewById(R.id.btn_filter_start_date);
        btnEndDate = findViewById(R.id.btn_filter_end_date);
        btnMinDur = findViewById(R.id.btn_filter_min_dur);
        btnMaxDur = findViewById(R.id.btn_filter_max_dur);

        btnApply = findViewById(R.id.btn_filter_apply);
        btnClear = findViewById(R.id.btn_filter_clear);

        setupSpinners();
        restorePreviousState();

        btnStartDate.setOnClickListener(v -> pickDate(true));
        btnEndDate.setOnClickListener(v -> pickDate(false));
        btnMinDur.setOnClickListener(v -> pickDuration(true));
        btnMaxDur.setOnClickListener(v -> pickDuration(false));

        btnApply.setOnClickListener(v -> applyFilters());
        btnClear.setOnClickListener(v -> clearFilters());
    }

    private void setupSpinners() {
        List<String> sports = new ArrayList<>();
        sports.add("Any");
        for (SportType type : SportType.values()) sports.add(type.getDisplayName());
        spinnerSport.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sports));

        List<String> levels = new ArrayList<>();
        levels.add("Any");
        for (DifficultyLevel level : DifficultyLevel.values()) levels.add(level.getDisplayName());
        spinnerLevel.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, levels));
    }

    private void pickDate(boolean isStart) {
        Calendar c = Calendar.getInstance();
        Long existingDate = isStart ? currentStartDate : currentEndDate;
        if (existingDate != null) c.setTimeInMillis(existingDate);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            if(isStart) selected.set(year, month, day, 0, 0, 0);
            else selected.set(year, month, day, 23, 59, 59);

            if (isStart) {
                currentStartDate = selected.getTimeInMillis();
                btnStartDate.setText(String.format("%02d/%02d/%d", day, month + 1, year));
            } else {
                currentEndDate = selected.getTimeInMillis();
                btnEndDate.setText(String.format("%02d/%02d/%d", day, month + 1, year));
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        // הוספת ההגבלות (Constraints)
        long today = System.currentTimeMillis() - 1000; // הורדת שניה כדי למנוע קריסות פנימיות באנדרואיד

        if (isStart) {
            datePickerDialog.getDatePicker().setMinDate(today); // אי אפשר לבחור עבר
            if (currentEndDate != null) {
                datePickerDialog.getDatePicker().setMaxDate(currentEndDate); // אי אפשר לחצות את תאריך הסיום
            }
        } else {
            if (currentStartDate != null) {
                datePickerDialog.getDatePicker().setMinDate(currentStartDate); // אי אפשר לבחור לפני תאריך ההתחלה
            } else {
                datePickerDialog.getDatePicker().setMinDate(today); // אי אפשר לבחור עבר
            }
        }

        datePickerDialog.show();
    }

    private void pickDuration(boolean isMin) {
        int initialHours = 0;
        int initialMinutes = 0;

        Integer currentVal = isMin ? currentMinDur : currentMaxDur;
        if (currentVal != null) {
            initialHours = currentVal / 60;
            initialMinutes = currentVal % 60;
        }

        // שימוש ב-TimePickerDialog כדי לדמות בורר זמן נוח לאנדרואיד (Hours & Minutes)
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            int totalMinutes = (hourOfDay * 60) + minute;

            if (isMin) {
                currentMinDur = totalMinutes;
                btnMinDur.setText(formatDuration(totalMinutes));

                // מוודא שהמינימום לא עוקף את המקסימום
                if (currentMaxDur != null && currentMaxDur < currentMinDur) {
                    currentMaxDur = currentMinDur;
                    btnMaxDur.setText(formatDuration(currentMaxDur));
                }
            } else {
                currentMaxDur = totalMinutes;
                btnMaxDur.setText(formatDuration(totalMinutes));

                // מוודא שהמקסימום לא יורד מהמינימום
                if (currentMinDur != null && currentMinDur > currentMaxDur) {
                    currentMinDur = currentMaxDur;
                    btnMinDur.setText(formatDuration(currentMinDur));
                }
            }
        }, initialHours, initialMinutes, true); // true = 24 Hour format

        timePickerDialog.setTitle(isMin ? "Select Min Duration" : "Select Max Duration");
        timePickerDialog.show();
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes == 0) return "0 mins";
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours > 0 && mins > 0) return hours + "h " + mins + "m";
        if (hours > 0) return hours + "h";
        return mins + "m";
    }

    private void restorePreviousState() {
        if (currentLocation != null) etLocation.setText(currentLocation);

        if (currentMinDur != null) btnMinDur.setText(formatDuration(currentMinDur));
        if (currentMaxDur != null) btnMaxDur.setText(formatDuration(currentMaxDur));

        if (currentStartDate != null) {
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(currentStartDate);
            btnStartDate.setText(String.format("%02d/%02d/%d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)));
        }
        if (currentEndDate != null) {
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(currentEndDate);
            btnEndDate.setText(String.format("%02d/%02d/%d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)));
        }
        if (currentSport != null) spinnerSport.setSelection(currentSport.ordinal() + 1);
        if (currentLevel != null) spinnerLevel.setSelection(currentLevel.ordinal() + 1);
    }

    private void applyFilters() {
        int sportPos = spinnerSport.getSelectedItemPosition();
        SportType selectedSport = (sportPos > 0) ? SportType.values()[sportPos - 1] : null;

        int levelPos = spinnerLevel.getSelectedItemPosition();
        DifficultyLevel selectedLevel = (levelPos > 0) ? DifficultyLevel.values()[levelPos - 1] : null;

        String location = etLocation.getText().toString().trim();

        if (listener != null) {
            listener.onFilterApplied(selectedSport, selectedLevel, location, currentStartDate, currentEndDate, currentMinDur, currentMaxDur);
        }
        dismiss();
    }

    private void clearFilters() {
        spinnerSport.setSelection(0);
        spinnerLevel.setSelection(0);
        etLocation.setText("");
        currentStartDate = null;
        currentEndDate = null;
        currentMinDur = null;
        currentMaxDur = null;
        btnStartDate.setText("Start Date");
        btnEndDate.setText("End Date");
        btnMinDur.setText("Min Duration");
        btnMaxDur.setText("Max Duration");
    }
}