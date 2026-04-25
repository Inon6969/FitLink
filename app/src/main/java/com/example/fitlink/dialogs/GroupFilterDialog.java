package com.example.fitlink.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.fitlink.R;
import com.example.fitlink.enums.DifficultyLevel;
import com.example.fitlink.enums.SportType;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class GroupFilterDialog extends Dialog {

    private final OnFilterAppliedListener listener;
    // משתנים לשמירת המצב הקודם של הסינון
    private final SportType currentSport;
    private final DifficultyLevel currentLevel;
    private final String currentLocation;
    private Spinner spinnerSport, spinnerLevel;
    private EditText etLocation;

    public GroupFilterDialog(Context context, SportType currentSport, DifficultyLevel currentLevel, String currentLocation, OnFilterAppliedListener listener) {
        super(context);
        this.currentSport = currentSport;
        this.currentLevel = currentLevel;
        this.currentLocation = currentLocation;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filter_groups);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        spinnerSport = findViewById(R.id.spinner_filter_sport);
        spinnerLevel = findViewById(R.id.spinner_filter_level);
        etLocation = findViewById(R.id.edit_filter_location);
        MaterialButton btnApply = findViewById(R.id.btn_filter_apply);
        MaterialButton btnClear = findViewById(R.id.btn_filter_clear);

        setupSpinners();
        restorePreviousState();

        btnApply.setOnClickListener(v -> applyFilters());
        btnClear.setOnClickListener(v -> clearFilters());
    }

    private void setupSpinners() {
        // רשימת סוגי ספורט כולל אפשרות "הכל"
        List<String> sports = new ArrayList<>();
        sports.add("Any Sport");
        for (SportType type : SportType.values()) sports.add(type.getDisplayName());
        spinnerSport.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sports));

        // רשימת רמות כולל אפשרות "הכל"
        List<String> levels = new ArrayList<>();
        levels.add("Any Level");
        for (DifficultyLevel level : DifficultyLevel.values()) levels.add(level.getDisplayName());
        spinnerLevel.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, levels));
    }

    private void restorePreviousState() {
        if (currentLocation != null) etLocation.setText(currentLocation);

        if (currentSport != null) {
            // הוסף 1 בגלל שאפשרות ה-"Any" נמצאת במיקום 0
            spinnerSport.setSelection(currentSport.ordinal() + 1);
        }
        if (currentLevel != null) {
            spinnerLevel.setSelection(currentLevel.ordinal() + 1);
        }
    }

    private void applyFilters() {
        int sportPos = spinnerSport.getSelectedItemPosition();
        SportType selectedSport = (sportPos > 0) ? SportType.values()[sportPos - 1] : null;

        int levelPos = spinnerLevel.getSelectedItemPosition();
        DifficultyLevel selectedLevel = (levelPos > 0) ? DifficultyLevel.values()[levelPos - 1] : null;

        String location = etLocation.getText().toString().trim();

        if (listener != null) {
            listener.onFilterApplied(selectedSport, selectedLevel, location);
        }
        dismiss();
    }

    private void clearFilters() {
        spinnerSport.setSelection(0);
        spinnerLevel.setSelection(0);
        etLocation.setText("");
    }

    public interface OnFilterAppliedListener {
        void onFilterApplied(SportType sportType, DifficultyLevel level, String location);
    }
}