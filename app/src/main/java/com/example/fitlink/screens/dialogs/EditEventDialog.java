package com.example.fitlink.screens.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.fitlink.R;
import com.example.fitlink.models.Event;
import com.example.fitlink.models.Location;
import com.example.fitlink.screens.EventDetailsActivity;
import com.example.fitlink.screens.GroupDashboardActivity;
import com.example.fitlink.screens.MapPickerActivity;
import com.example.fitlink.services.DatabaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class EditEventDialog extends Dialog {

    private final Context context;
    private final Event currentEvent;
    private final DatabaseService databaseService;
    private final OnEventUpdatedListener listener;

    private MaterialButton btnDate, btnTime, btnDuration, btnLocation;
    private TextInputEditText inputTitle, inputDescription, inputMaxParticipants;

    private final Calendar eventCalendar = Calendar.getInstance();
    private boolean isDateSet = true;
    private boolean isTimeSet = true;
    private long selectedDurationMillis = 0;

    private String selectedAddress = "";
    private double selectedLat = 0;
    private double selectedLng = 0;

    public interface OnEventUpdatedListener {
        void onEventUpdated(Event updatedEvent);
    }

    public EditEventDialog(Context context, Event currentEvent, OnEventUpdatedListener listener) {
        super(context);
        this.context = context;
        this.currentEvent = currentEvent;
        this.listener = listener;
        this.databaseService = DatabaseService.getInstance();
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_edit_event);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        inputTitle = findViewById(R.id.inputEditGroupEventTitle);
        inputDescription = findViewById(R.id.inputEditGroupEventDescription);
        inputMaxParticipants = findViewById(R.id.inputEditGroupEventMaxParticipants);

        btnDate = findViewById(R.id.btnEditGroupEventDate);
        btnTime = findViewById(R.id.btnEditGroupEventTime);
        btnDuration = findViewById(R.id.btnEditGroupEventDuration);
        btnLocation = findViewById(R.id.btnEditGroupEventLocation);

        MaterialButton btnSave = findViewById(R.id.btnEditGroupEventSave);
        MaterialButton btnCancel = findViewById(R.id.btnEditGroupEventCancel);

        prefillData();
        setupPickers();

        btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapPickerActivity.class);
            if (context instanceof EventDetailsActivity) {
                ((EventDetailsActivity) context).getMapPickerLauncher().launch(intent);
            } else if (context instanceof GroupDashboardActivity) {
                ((GroupDashboardActivity) context).getMapPickerLauncher().launch(intent);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void prefillData() {
        inputTitle.setText(currentEvent.getTitle());
        inputDescription.setText(currentEvent.getDescription() != null ? currentEvent.getDescription() : "");
        inputMaxParticipants.setText(String.valueOf(currentEvent.getMaxParticipants()));

        if (currentEvent.getStartTimestamp() > 0) {
            eventCalendar.setTimeInMillis(currentEvent.getStartTimestamp());
            btnDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(eventCalendar.getTime()));
            btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", eventCalendar.get(Calendar.HOUR_OF_DAY), eventCalendar.get(Calendar.MINUTE)));
        }

        selectedDurationMillis = currentEvent.getDurationMillis();
        if (selectedDurationMillis > 0) {
            int hours = (int) (selectedDurationMillis / (1000 * 60 * 60));
            int minutes = (int) ((selectedDurationMillis / (1000 * 60)) % 60);
            if (hours > 0 && minutes > 0) btnDuration.setText(hours + "h " + minutes + "m");
            else if (hours > 0) btnDuration.setText(hours + "h");
            else btnDuration.setText(minutes + "m");
        }

        if (currentEvent.getLocation() != null && currentEvent.getLocation().getAddress() != null) {
            selectedAddress = currentEvent.getLocation().getAddress();
            selectedLat = currentEvent.getLocation().getLatitude();
            selectedLng = currentEvent.getLocation().getLongitude();
            btnLocation.setText(selectedAddress);
        }
    }

    private void setupPickers() {
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

        btnTime.setOnClickListener(v -> {
            new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                eventCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                eventCalendar.set(Calendar.MINUTE, minute);
                eventCalendar.set(Calendar.SECOND, 0);
                isTimeSet = true;
                btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, eventCalendar.get(Calendar.HOUR_OF_DAY), eventCalendar.get(Calendar.MINUTE), true).show();
        });

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

        int maxParticipants = maxPartStr.isEmpty() ? 0 : Integer.parseInt(maxPartStr);
        Location location = new Location(selectedAddress, selectedLat, selectedLng);

        // עדכון האובייקט הקיים (שומרים על סוג הספורט והרמה שהיו)
        currentEvent.setTitle(title);
        currentEvent.setDescription(description);
        currentEvent.setStartTimestamp(startTimestamp);
        currentEvent.setDurationMillis(selectedDurationMillis);
        currentEvent.setLocation(location);
        currentEvent.setMaxParticipants(maxParticipants);

        databaseService.updateEvent(currentEvent, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(context, "Event Updated Successfully!", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onEventUpdated(currentEvent);
                }
                dismiss();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(context, "Failed to update event", Toast.LENGTH_SHORT).show();
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