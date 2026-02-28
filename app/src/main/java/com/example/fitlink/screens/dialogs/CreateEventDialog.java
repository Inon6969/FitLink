package com.example.fitlink.screens.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.Group;
import com.example.fitlink.models.Location;
import com.example.fitlink.screens.GroupDashboardActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
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

    private MaterialButton btnDate, btnTime, btnDuration, btnLocation;
    private TextInputEditText inputTitle, inputDescription, inputMaxParticipants;

    private final Calendar eventCalendar = Calendar.getInstance();
    private boolean isDateSet = false;
    private boolean isTimeSet = false;
    private long selectedDurationMillis = 0; // משתנה לשמירת משך הזמן

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
        inputMaxParticipants = findViewById(R.id.inputEventMaxParticipants);

        btnDate = findViewById(R.id.btnEventDate);
        btnTime = findViewById(R.id.btnEventTime);
        btnDuration = findViewById(R.id.btnEventDuration);
        btnLocation = findViewById(R.id.btnEventLocation);

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
            new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                eventCalendar.set(Calendar.YEAR, year);
                eventCalendar.set(Calendar.MONTH, month);
                eventCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                isDateSet = true;
                btnDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(eventCalendar.getTime()));
            }, eventCalendar.get(Calendar.YEAR), eventCalendar.get(Calendar.MONTH), eventCalendar.get(Calendar.DAY_OF_MONTH)).show();
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

        // בחירת משך זמן (Duration) דרך דיאלוג מובנה
        btnDuration.setOnClickListener(v -> {
            int currentHours = (int) (selectedDurationMillis / (1000 * 60 * 60));
            int currentMinutes = (int) ((selectedDurationMillis / (1000 * 60)) % 60);

            // אם טרם נבחר זמן, נציע כברירת מחדל שעה אחת
            if (selectedDurationMillis == 0) {
                currentHours = 1;
                currentMinutes = 0;
            }

            TimePickerDialog durationPicker = new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                // המרת השעות והדקות שנבחרו למילישניות (שעה = 60 דקות, דקה = 60,000 מילישניות)
                selectedDurationMillis = (hourOfDay * 60L + minute) * 60L * 1000L;

                // עיצוב הטקסט על הכפתור כדי שיראה טוב
                String formattedDuration;
                if (hourOfDay > 0 && minute > 0) {
                    formattedDuration = hourOfDay + "h " + minute + "m";
                } else if (hourOfDay > 0) {
                    formattedDuration = hourOfDay + "h";
                } else {
                    formattedDuration = minute + "m";
                }

                // טיפול במקרה של בחירת 0 שעות ו-0 דקות
                if (selectedDurationMillis == 0) {
                    btnDuration.setText("Duration");
                    Toast.makeText(context, "Duration cannot be zero", Toast.LENGTH_SHORT).show();
                } else {
                    btnDuration.setText(formattedDuration);
                }

            }, currentHours, currentMinutes, true); // true = 24 Hour format

            durationPicker.setTitle("Select Duration");
            durationPicker.show();
        });
    }

    private void saveEvent() {
        String title = Objects.requireNonNull(inputTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(inputDescription.getText()).toString().trim();
        String maxPartStr = Objects.requireNonNull(inputMaxParticipants.getText()).toString().trim();

        if (title.isEmpty()) {
            inputTitle.setError("Title is required");
            return;
        }
        if (!isDateSet || !isTimeSet) {
            Toast.makeText(context, "Please select both date and time", Toast.LENGTH_SHORT).show();
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

        int maxParticipants = maxPartStr.isEmpty() ? 0 : Integer.parseInt(maxPartStr);
        long startTimestamp = eventCalendar.getTimeInMillis();
        Location location = new Location(selectedAddress, selectedLat, selectedLng);
        String currentUserId = SharedPreferencesUtil.getUserId(context);

        Event newEvent = new Event(null, group.getId(), title, description, group.getSportType(),
                group.getLevel(), startTimestamp, selectedDurationMillis, location,
                currentUserId, maxParticipants);

        databaseService.createNewEvent(newEvent, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(context, "Event Scheduled Successfully!", Toast.LENGTH_SHORT).show();
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