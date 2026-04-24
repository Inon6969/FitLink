package com.example.fitlink.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fitlink.services.FitLinkNotificationService;

public class EventReminderWorker extends Worker {

    public EventReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // שולפים את הנתונים שהעברנו למשימה (מזהה אירוע ושם אירוע)
        String eventId = getInputData().getString("eventId");
        String eventTitle = getInputData().getString("eventTitle");

        if (eventId != null && eventTitle != null) {
            FitLinkNotificationService.getInstance(getApplicationContext())
                    .showEventReminderNotification(eventId, eventTitle);
        }

        return Result.success();
    }
}