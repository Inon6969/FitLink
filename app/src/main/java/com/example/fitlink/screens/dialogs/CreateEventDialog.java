package com.example.fitlink.screens.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.Location;
import com.example.fitlink.screens.GroupDashboardActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.EventReminderScheduler; // הוספנו את מחלקת התזמון
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class CreateEventDialog extends Dialog {

    private final Context context;
    private final Group group;
    private final DatabaseService databaseService;

    private MaterialButton btnDate, btnTime, btnDuration, btnLocation, btnMaxParticipants;
    private TextInputEditText inputTitle, inputDescription;

    private final Calendar eventCalendar = Calendar.getInstance();
    private boolean isDateSet = false;
    private boolean isTimeSet = false;
    private long selectedDurationMillis = 0;
    private int selectedMaxParticipants = 0; // משתנה לשמירת כמות המשתתפים

    private String selectedAddress = "";
    private double selectedLat = 0;
    private double selectedLng = 0;

    public CreateEventDialog(Context context, Group group) {
        super(context);
        this.context = context;
        this.group = group;
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_create_event);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        inputTitle = findViewById(R.id.inputEventTitle);
        inputDescription = findViewById(R.id.inputEventDescription);

        btnDate = findViewById(R.id.btnEventDate);
        btnTime = findViewById(R.id.btnEventTime);
        btnDuration = findViewById(R.id.btnEventDuration);
        btnLocation = findViewById(R.id.btnEventLocation);
        btnMaxParticipants = findViewById(R.id.btnEventMaxParticipants);

        MaterialButton btnSave = findViewById(R.id.btnEventSave);
        MaterialButton btnCancel = findViewById(R.id.btnEventCancel);

        setupPickers();

        btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapPickerActivity.class);
            if (context instanceof GroupDashboardActivity) {
                ((GroupDashboardActivity) context).getMapPickerLauncher().launch(intent);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void setupPickers() {
        // בחירת תאריך
        btnDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                eventCalendar.set(Calendar.YEAR, year);
                eventCalendar.set(Calendar.MONTH, month);
                eventCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                isDateSet = true;
                btnDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(eventCalendar.getTime()));
            }, eventCalendar.get(Calendar.YEAR), eventCalendar.get(Calendar.MONTH), eventCalendar.get(Calendar.DAY_OF_MONTH));

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        // בחירת שעת התחלה
        btnTime.setOnClickListener(v -> {
            new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                eventCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                eventCalendar.set(Calendar.MINUTE, minute);
                eventCalendar.set(Calendar.SECOND, 0);
                isTimeSet = true;
                btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, eventCalendar.get(Calendar.HOUR_OF_DAY), eventCalendar.get(Calendar.MINUTE), true).show();
        });

        // בחירת משך זמן
        btnDuration.setOnClickListener(v -> {
            int currentHours = (int) (selectedDurationMillis / (1000 * 60 * 60));
            int currentMinutes = (int) ((selectedDurationMillis / (1000 * 60)) % 60);

            if (selectedDurationMillis == 0) {
                currentHours = 1;
                currentMinutes = 0;
            }

            TimePickerDialog durationPicker = new TimePickerDialog(context, (view, hourOfDay, minute) -> {
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
                    Toast.makeText(context, "Duration cannot be zero", Toast.LENGTH_SHORT).show();
                } else {
                    btnDuration.setText(formattedDuration);
                }

            }, currentHours, currentMinutes, true);

            durationPicker.setTitle("Select Duration");
            durationPicker.show();
        });

        // בחירת כמות משתתפים מקסימלית (NumberPicker) מעוצב ומוגבל לכמות חברי הקבוצה
        btnMaxParticipants.setOnClickListener(v -> {

            // 1. קבלת כמות החברים המקסימלית בקבוצה (אם מסיבה כלשהי ה-Map ריק, נגדיר לפחות 1)
            int maxMembers = (group.getMembers() != null) ? group.getMembers().size() : 20;
            if (maxMembers < 1) maxMembers = 1;

            NumberPicker numberPicker = new NumberPicker(context);
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(maxMembers); // מוגבל לכמות האנשים בקבוצה
            numberPicker.setValue(selectedMaxParticipants);
            numberPicker.setWrapSelectorWheel(true); // מאפשר גלילה מעגלית יפה

            // 2. תצוגה מותאמת אישית לערך 0 - רק "Any" בלי המספר
            String[] displayedValues = new String[maxMembers + 1];
            displayedValues[0] = "Any";
            for (int i = 1; i <= maxMembers; i++) {
                displayedValues[i] = String.valueOf(i);
            }
            numberPicker.setDisplayedValues(displayedValues);

            // 3. יצירת מעטפת (Container) כדי לתת לגלגלת שוליים ועיצוב מרווח יותר
            android.widget.FrameLayout container = new android.widget.FrameLayout(context);
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = android.view.Gravity.CENTER;
            params.setMargins(0, 50, 0, 50); // מרווח נשימה מלמעלה ולמטה
            numberPicker.setLayoutParams(params);
            container.addView(numberPicker);

            // יצירת והצגת חלון הדיאלוג
            new AlertDialog.Builder(context)
                    .setTitle("Max Participants")
                    .setView(container) // משתמשים במעטפת המעוצבת שלנו במקום בגלגלת הישירה
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
        });
    }

    private void saveEvent() {
        String title = Objects.requireNonNull(inputTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(inputDescription.getText()).toString().trim();

        if (title.isEmpty()) {
            inputTitle.setError("Title is required");
            return;
        }
        if (!isDateSet || !isTimeSet) {
            Toast.makeText(context, "Please select both date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        long startTimestamp = eventCalendar.getTimeInMillis();

        if (startTimestamp <= System.currentTimeMillis()) {
            Toast.makeText(context, "Event time must be in the future", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedDurationMillis <= 0) {
            Toast.makeText(context, "Please select the event duration", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAddress.isEmpty()) {
            Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        Location location = new Location(selectedAddress, selectedLat, selectedLng);
        String currentUserId = SharedPreferencesUtil.getUserId(context);

        Event newEvent = new Event(null, group.getId(), title, description, group.getSportType(),
                group.getLevel(), startTimestamp, selectedDurationMillis, location,
                currentUserId, selectedMaxParticipants);

        databaseService.createNewEvent(newEvent, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(context, "Event Scheduled Successfully!", Toast.LENGTH_SHORT).show();

                // תזמון ההתראה עבור היוצר (שמצורף לאירוע אוטומטית)
                EventReminderScheduler.scheduleReminder(context, newEvent);

                dismiss();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(context, "Failed to schedule event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateLocationDetails(String address, double lat, double lng) {
        this.selectedAddress = address;
        this.selectedLat = lat;
        this.selectedLng = lng;
        if (btnLocation != null) {
            btnLocation.setText(address);
        }
    }
}