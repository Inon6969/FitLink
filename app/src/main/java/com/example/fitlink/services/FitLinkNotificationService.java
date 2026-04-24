package com.example.fitlink.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.fitlink.R;
import com.example.fitlink.screens.GroupChatActivity;
import com.example.fitlink.screens.MainActivity;

public class FitLinkNotificationService {

    // --- קבועים לערוצי ההתראות השונים ---

    // 1. בקשות הצטרפות
    public static final String GROUP_REQUESTS_CHANNEL_ID = "group_requests_high_priority";
    // 2. הודעות בצ'אט
    public static final String CHAT_CHANNEL_ID = "group_chat_messages";
    // 3. אירועים חדשים בקבוצה
    public static final String EVENTS_CHANNEL_ID = "group_new_events";
    // 4. תזכורות לאירועים (24 שעות לפני)
    public static final String REMINDER_CHANNEL_ID = "event_reminders";
    private static final String GROUP_REQUESTS_GROUP = "com.example.fitlink.GROUP_REQUESTS";
    private static final String CHAT_GROUP = "com.example.fitlink.CHAT_MESSAGES";
    private static final String EVENTS_GROUP = "com.example.fitlink.NEW_EVENTS";
    private static final String REMINDER_GROUP = "com.example.fitlink.REMINDERS";


    private static FitLinkNotificationService instance;
    private final Context context;
    private final NotificationManagerCompat manager;

    private FitLinkNotificationService(Context context) {
        this.context = context.getApplicationContext();
        this.manager = NotificationManagerCompat.from(this.context);
        createChannels();
    }

    public static FitLinkNotificationService getInstance(Context context) {
        if (instance == null) {
            instance = new FitLinkNotificationService(context);
        }
        return instance;
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager systemManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (systemManager == null) return;

            // 1. יצירת ערוץ לבקשות הצטרפות
            NotificationChannel requestChannel = new NotificationChannel(
                    GROUP_REQUESTS_CHANNEL_ID,
                    "Group Join Requests",
                    NotificationManager.IMPORTANCE_HIGH
            );
            requestChannel.setDescription("Notifications for users requesting to join your groups");
            requestChannel.enableVibration(true);
            requestChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            systemManager.createNotificationChannel(requestChannel);

            // 2. יצירת ערוץ להודעות צ'אט
            NotificationChannel chatChannel = new NotificationChannel(
                    CHAT_CHANNEL_ID,
                    "Group Chat Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("Notifications for new messages in your groups");
            chatChannel.enableVibration(true);
            systemManager.createNotificationChannel(chatChannel);

            // 3. יצירת ערוץ לאירועים חדשים
            NotificationChannel eventChannel = new NotificationChannel(
                    EVENTS_CHANNEL_ID,
                    "New Group Events",
                    NotificationManager.IMPORTANCE_HIGH
            );
            eventChannel.setDescription("Notifications when a new event is created in your groups");
            eventChannel.enableVibration(true);
            systemManager.createNotificationChannel(eventChannel);

            // 4. יצירת ערוץ לתזכורות אירועים
            NotificationChannel reminderChannel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Reminders 24 hours before your events start");
            reminderChannel.enableVibration(true);
            systemManager.createNotificationChannel(reminderChannel);
        }
    }

    // ==========================================
    // הפעלת התראות בפועל
    // ==========================================

    // --- 1. התראה על בקשת הצטרפות לקבוצה ---
    public void showJoinRequestNotification(String groupName, String requesterId) {
        String title = "New Join Request!";
        String message = "Someone wants to join " + groupName;
        int notificationId = (groupName + requesterId).hashCode();

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, GROUP_REQUESTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_REQUESTS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notificationId, builder.build());
            showSummaryNotification();
        }
    }

    private void showSummaryNotification() {
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, GROUP_REQUESTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(new NotificationCompat.InboxStyle())
                .setGroup(GROUP_REQUESTS_GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(GROUP_REQUESTS_GROUP.hashCode(), summaryBuilder.build());
        }
    }

    // --- 2. התראה על הודעה חדשה בצ'אט ---
    public void showChatMessageNotification(String groupId, String groupName, String senderName, String messageText) {
        int notificationId = (groupId + senderName + messageText).hashCode();

        Intent intent = new Intent(context, GroupChatActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(groupName + " - " + senderName)
                .setContentText(messageText)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(CHAT_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notificationId, builder.build());
        }
    }

    // --- 3. התראה על אירוע קבוצתי חדש ---
    public void showNewEventNotification(String groupName, String eventTitle, String eventId) {
        int notificationId = eventId.hashCode();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, EVENTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("New Event in " + groupName + "!")
                .setContentText(eventTitle)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(EVENTS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notificationId, builder.build());
        }
    }

    // --- 4. התראה לתזכורת (24 שעות לפני האירוע) ---
    public void showEventReminderNotification(String eventId, String eventTitle) {
        int notificationId = eventId.hashCode();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Event Tomorrow!")
                .setContentText("Don't forget: " + eventTitle + " is starting in 24 hours.")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(REMINDER_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notificationId, builder.build());
        }
    }
}