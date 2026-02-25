package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.fitlink.R;
import com.example.fitlink.models.DifficultyLevel;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.Location;
import com.example.fitlink.models.SportType;
import com.example.fitlink.screens.GroupsListActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class AddGroupDialog extends Dialog {

    private final Context context;
    private final DatabaseService databaseService;

    // UI Elements
    private MaterialButton btnOpenMap;
    private TextInputEditText inputName;
    private TextInputEditText inputDescription;
    private Spinner spinnerSport;
    private ChipGroup chipGroupLevel;

    // Selected location data
    private String selectedAddress = "";
    private double selectedLat = 0;
    private double selectedLng = 0;

    public AddGroupDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_group);

        // Initialize components
        inputName = findViewById(R.id.inputAddGroupName);
        inputDescription = findViewById(R.id.inputAddGroupDescription);
        btnOpenMap = findViewById(R.id.btnOpenMapPicker);
        spinnerSport = findViewById(R.id.spinnerAddGroupSportType);
        chipGroupLevel = findViewById(R.id.chipGroupAddGroupLevel);
        MaterialButton btnSave = findViewById(R.id.btnAddGroupSave);
        MaterialButton btnCancel = findViewById(R.id.btnAddGroupCancel);

        // Setup sport type spinner
        ArrayAdapter<SportType> sportAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, SportType.values());
        sportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSport.setAdapter(sportAdapter);

        // Map button click
        btnOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapPickerActivity.class);
            if (context instanceof GroupsListActivity) {
                ((GroupsListActivity) context).getMapPickerLauncher().launch(intent);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String name = Objects.requireNonNull(inputName.getText()).toString().trim();

            String description = "";
            if (inputDescription.getText() != null) {
                description = inputDescription.getText().toString().trim();
            }

            SportType selectedSport = (SportType) spinnerSport.getSelectedItem();

            // Get level from the selected chip
            int checkedChipId = chipGroupLevel.getCheckedChipId();
            Chip selectedChip = findViewById(checkedChipId);
            String levelText = (selectedChip != null) ? selectedChip.getText().toString() : "Beginner";

            // Map string to DifficultyLevel Enum
            DifficultyLevel selectedLevel;
            if (levelText.equals("Intermediate")) {
                selectedLevel = DifficultyLevel.INTERMEDIATE;
            } else if (levelText.equals("Advanced")) {
                selectedLevel = DifficultyLevel.ADVANCED;
            } else {
                selectedLevel = DifficultyLevel.BEGINNER;
            }

            // Validation
            if (name.isEmpty()) {
                inputName.setError("Please enter a group name");
                return;
            }

            if (selectedAddress.isEmpty()) {
                Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_LONG).show();
                return;
            }

            Location locationObj = new Location(selectedAddress, selectedLat, selectedLng);
            String adminId = SharedPreferencesUtil.getUserId(context);

            // Create Group using the Enum
            Group newGroup = new Group(null, name, description, selectedSport, selectedLevel, locationObj, adminId);

            // Save to Firebase
            databaseService.createNewGroup(newGroup, new DatabaseService.DatabaseCallback<>() {
                @Override
                public void onCompleted(Void object) {
                    Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show();
                    dismiss();
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public void updateLocationDetails(String address, double lat, double lng) {
        this.selectedAddress = address;
        this.selectedLat = lat;
        this.selectedLng = lng;

        if (btnOpenMap != null) {
            btnOpenMap.setText(address);
        }
    }
}