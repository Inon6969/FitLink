package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.fitlink.R;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.SportType;
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

    public AddGroupDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_group);

        // קישור רכיבי ה-UI מה-XML (dialog_add_group.xml)
        TextInputEditText inputName = findViewById(R.id.inputAddGroupName);
        TextInputEditText inputLocation = findViewById(R.id.inputAddGroupLocation);
        Spinner spinnerSport = findViewById(R.id.spinnerAddGroupSportType);
        ChipGroup chipGroupLevel = findViewById(R.id.chipGroupAddGroupLevel);
        MaterialButton btnSave = findViewById(R.id.btnAddGroupSave);
        MaterialButton btnCancel = findViewById(R.id.btnAddGroupCancel);

        // הגדרת הספינר לסוגי ספורט (מבוסס על ה-Enum שקיים אצלך)
        ArrayAdapter<SportType> sportAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, SportType.values());
        sportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSport.setAdapter(sportAdapter);

        // כפתור ביטול - פשוט סוגר את הדיאלוג
        btnCancel.setOnClickListener(v -> dismiss());

        // כפתור יצירה - שומר את הנתונים ב-Firebase
        btnSave.setOnClickListener(v -> {
            String name = Objects.requireNonNull(inputName.getText()).toString().trim();
            String location = Objects.requireNonNull(inputLocation.getText()).toString().trim();
            SportType selectedSport = (SportType) spinnerSport.getSelectedItem();

            // מציאת הרמה הנבחרת מתוך ה-ChipGroup
            int checkedChipId = chipGroupLevel.getCheckedChipId();
            Chip selectedChip = findViewById(checkedChipId);
            String level = (selectedChip != null) ? selectedChip.getText().toString() : "Beginner";

            // ולידציה בסיסית
            if (name.isEmpty() || location.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // קבלת ה-ID של המשתמש הנוכחי (הוא יהיה ה-Admin)
            String adminId = SharedPreferencesUtil.getUserId(context);

            // יצירת אובייקט קבוצה חדש (ID נוצר אוטומטית ב-Service)
            Group newGroup = new Group(null, name, "", selectedSport, level, location, adminId);

            // ביצוע העדכון ב-DatabaseService (העדכון האטומי שדיברנו עליו)
            databaseService.createNewGroup(newGroup, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show();
                    dismiss();
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(context, "Failed to create group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}