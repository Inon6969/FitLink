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
import com.example.fitlink.screens.GroupDashboardActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class EditGroupDialog extends Dialog {

    private final Context context;
    private final DatabaseService databaseService;
    private final Group currentGroup;
    private final OnGroupUpdatedListener listener;

    private MaterialButton btnOpenMap;
    private TextInputEditText inputName;
    private TextInputEditText inputDescription;
    private Spinner spinnerSport;
    private ChipGroup chipGroupLevel;

    private String selectedAddress = "";
    private double selectedLat = 0;
    private double selectedLng = 0;

    public interface OnGroupUpdatedListener {
        void onGroupUpdated(Group updatedGroup);
    }

    public EditGroupDialog(@NonNull Context context, Group currentGroup, OnGroupUpdatedListener listener) {
        super(context);
        this.context = context;
        this.currentGroup = currentGroup;
        this.listener = listener;
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_edit_group);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        inputName = findViewById(R.id.inputEditGroupName);
        inputDescription = findViewById(R.id.inputEditGroupDescription);
        btnOpenMap = findViewById(R.id.btnEditOpenMapPicker);
        spinnerSport = findViewById(R.id.spinnerEditGroupSportType);
        chipGroupLevel = findViewById(R.id.chipGroupEditGroupLevel);
        MaterialButton btnSave = findViewById(R.id.btnEditGroupSave);
        MaterialButton btnCancel = findViewById(R.id.btnEditGroupCancel);

        // הגדרת ספינר סוג ספורט
        ArrayAdapter<SportType> sportAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, SportType.values());
        sportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSport.setAdapter(sportAdapter);

        // מילוי הנתונים הקיימים
        prefillData(sportAdapter);

        btnOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapPickerActivity.class);
            if (context instanceof GroupDashboardActivity) {
                ((GroupDashboardActivity) context).getMapPickerLauncher().launch(intent);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void prefillData(ArrayAdapter<SportType> sportAdapter) {
        inputName.setText(currentGroup.getName());
        inputDescription.setText(currentGroup.getDescription());

        if (currentGroup.getLocation() != null) {
            selectedAddress = currentGroup.getLocation().getAddress();
            selectedLat = currentGroup.getLocation().getLatitude();
            selectedLng = currentGroup.getLocation().getLongitude();
            btnOpenMap.setText(selectedAddress);
        }

        if (currentGroup.getSportType() != null) {
            int spinnerPosition = sportAdapter.getPosition(currentGroup.getSportType());
            spinnerSport.setSelection(spinnerPosition);
        }

        if (currentGroup.getLevel() != null) {
            if (currentGroup.getLevel() == DifficultyLevel.BEGINNER) {
                chipGroupLevel.check(R.id.chipEditBeginner);
            } else if (currentGroup.getLevel() == DifficultyLevel.INTERMEDIATE) {
                chipGroupLevel.check(R.id.chipEditIntermediate);
            } else if (currentGroup.getLevel() == DifficultyLevel.ADVANCED) {
                chipGroupLevel.check(R.id.chipEditAdvanced);
            }
        }
    }

    private void saveChanges() {
        String name = Objects.requireNonNull(inputName.getText()).toString().trim();
        String description = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
        SportType selectedSport = (SportType) spinnerSport.getSelectedItem();

        int checkedChipId = chipGroupLevel.getCheckedChipId();
        Chip selectedChip = findViewById(checkedChipId);
        String levelText = (selectedChip != null) ? selectedChip.getText().toString() : "Beginner";

        DifficultyLevel selectedLevel;
        if (levelText.equals("Intermediate")) {
            selectedLevel = DifficultyLevel.INTERMEDIATE;
        } else if (levelText.equals("Advanced")) {
            selectedLevel = DifficultyLevel.ADVANCED;
        } else {
            selectedLevel = DifficultyLevel.BEGINNER;
        }

        if (name.isEmpty()) {
            inputName.setError("Please enter a group name");
            return;
        }

        if (selectedAddress.isEmpty()) {
            Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_LONG).show();
            return;
        }

        Location locationObj = new Location(selectedAddress, selectedLat, selectedLng);

        // עדכון אובייקט הקבוצה
        currentGroup.setName(name);
        currentGroup.setDescription(description);
        currentGroup.setSportType(selectedSport);
        currentGroup.setLevel(selectedLevel);
        currentGroup.setLocation(locationObj);

        // שמירה במסד הנתונים
        databaseService.updateGroup(currentGroup, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(context, "Group updated successfully!", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onGroupUpdated(currentGroup);
                }
                dismiss();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(context, "Failed to update group", Toast.LENGTH_SHORT).show();
            }
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