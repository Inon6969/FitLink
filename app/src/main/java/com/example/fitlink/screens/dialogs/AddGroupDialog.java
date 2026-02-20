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

    // רכיבי UI
    private MaterialButton btnOpenMap;
    private TextInputEditText inputName;
    private Spinner spinnerSport;
    private ChipGroup chipGroupLevel;

    // נתוני מיקום שנבחרו
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

        // אתחול רכיבים
        inputName = findViewById(R.id.inputAddGroupName);
        btnOpenMap = findViewById(R.id.btnOpenMapPicker);
        spinnerSport = findViewById(R.id.spinnerAddGroupSportType);
        chipGroupLevel = findViewById(R.id.chipGroupAddGroupLevel);
        MaterialButton btnSave = findViewById(R.id.btnAddGroupSave);
        MaterialButton btnCancel = findViewById(R.id.btnAddGroupCancel);

        // הגדרת ספינר סוגי ספורט
        ArrayAdapter<SportType> sportAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, SportType.values());
        sportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSport.setAdapter(sportAdapter);

        // לחיצה על כפתור המפה
        btnOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapPickerActivity.class);
            if (context instanceof GroupsListActivity) {
                // הפעלה דרך הלאנצ'ר של ה-Activity כדי לקבל חזרה את המיקום
                ((GroupsListActivity) context).getMapPickerLauncher().launch(intent);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String name = Objects.requireNonNull(inputName.getText()).toString().trim();
            SportType selectedSport = (SportType) spinnerSport.getSelectedItem();

            // קבלת הרמה מה-Chip הנבחר
            int checkedChipId = chipGroupLevel.getCheckedChipId();
            Chip selectedChip = findViewById(checkedChipId);
            String level = (selectedChip != null) ? selectedChip.getText().toString() : "Beginner";

            // ולידציה - חובה להזין שם ולבחור מיקום במפה
            if (name.isEmpty()) {
                inputName.setError("Please enter a group name");
                return;
            }

            if (selectedAddress.isEmpty()) {
                Toast.makeText(context, "Please select a location on the map", Toast.LENGTH_LONG).show();
                return;
            }

            // יצירת אובייקט ה-Location והקבוצה
            Location locationObj = new Location(selectedAddress, selectedLat, selectedLng);
            String adminId = SharedPreferencesUtil.getUserId(context);

            Group newGroup = new Group(null, name, "", selectedSport, level, locationObj, adminId);

            // שמירה ל-Firebase
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

    /**
     * מתודה לעדכון פרטי המיקום - נקראת מה-GroupsListActivity לאחר חזרה מהמפה
     */
    public void updateLocationDetails(String address, double lat, double lng) {
        this.selectedAddress = address;
        this.selectedLat = lat;
        this.selectedLng = lng;

        // עדכון הטקסט על הכפתור כדי להראות למשתמש מה הוא בחר
        if (btnOpenMap != null) {
            btnOpenMap.setText(address);
        }
    }
}