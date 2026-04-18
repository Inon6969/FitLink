package com.example.fitlink.utils;

import android.content.Context;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.fitlink.models.Event;
import com.example.fitlink.workers.EventReminderWorker;

import java.util.concurrent.TimeUnit;

public class EventReminderScheduler {

    public static void scheduleReminder(Context context, Event event) {
        // חישוב הזמן (במילישניות) עד שנקודת ה-24 שעות לפני האירוע תגיע
        long currentTime = System.currentTimeMillis();
        long twentyFourHoursInMillis = 24 * 60 * 60 * 1000L;
        long targetTime = event.getStartTimestamp() - twentyFourHoursInMillis;
        long delayInMillis = targetTime - currentTime;

        // מוודאים שיש עוד יותר מ-24 שעות לאירוע. אם נשארו פחות מ-24 שעות, אין טעם לתזמן
        if (delayInMillis > 0) {
            Data inputData = new Data.Builder()
                    .putString("eventId", event.getId())
                    .putString("eventTitle", event.getTitle())
                    .build();

            OneTimeWorkRequest reminderRequest = new OneTimeWorkRequest.Builder(EventReminderWorker.class)
                    .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("reminder_" + event.getId()) // תג כדי שנוכל לבטל אם המשתמש עוזב את האירוע
                    .build();

            // משתמשים ב-REPLACE כדי שאם השעות ישתנו ויבוצע תזמון מחדש, הקודם יידרס
            WorkManager.getInstance(context).enqueueUniqueWork(
                    "work_reminder_" + event.getId(),
                    ExistingWorkPolicy.REPLACE,
                    reminderRequest
            );
        }
    }

    public static void cancelReminder(Context context, String eventId) {
        WorkManager.getInstance(context).cancelAllWorkByTag("reminder_" + eventId);
    }
}