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
import com.example.fitlink.screens.MainActivity; // אם יש לך מסך ניהול בקשות, שנה אליו

public class FitLinkNotificationService {

    public static final String GROUP_REQUESTS_CHANNEL_ID = "group_requests_high_priority";
    private static final String GROUP_REQUESTS_GROUP = "com.example.fitlink.GROUP_REQUESTS";
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
            NotificationChannel requestChannel = new NotificationChannel(
                    GROUP_REQUESTS_CHANNEL_ID,
                    "Group Join Requests",
                    NotificationManager.IMPORTANCE_HIGH // שונה מ-DEFAULT ל-HIGH כדי לאפשר פופ-אפ
            );
            requestChannel.setDescription("Notifications for users requesting to join your groups");

            // הפעלת רטט בצורה מפורשת (אופציונלי: אפשר גם להגדיר תבנית רטט מותאמת אישית)
            requestChannel.enableVibration(true);
            requestChannel.setVibrationPattern(new long[]{0, 500, 200, 500}); // השהייה, רטט, השהייה, רטט

            NotificationManager systemManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (systemManager != null) {
                systemManager.createNotificationChannel(requestChannel);
            }
        }
    }

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
                .setSmallIcon(R.drawable.ic_notification) // ודא שיש לך אייקון כזה
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_REQUESTS_GROUP)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // שונה ל-HIGH עבור מכשירים ישנים
                .setDefaults(NotificationCompat.DEFAULT_ALL); // מפעיל את צליל ברירת המחדל והרטט של המכשיר

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(notificationId, builder.build());
            showSummaryNotification();
        }
    }

    private void showSummaryNotification() {
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, GROUP_REQUESTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // כנ"ל לגבי האייקון
                .setStyle(new NotificationCompat.InboxStyle())
                .setGroup(GROUP_REQUESTS_GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(GROUP_REQUESTS_GROUP.hashCode(), summaryBuilder.build());
        }
    }
}