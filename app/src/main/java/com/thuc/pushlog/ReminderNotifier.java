package com.thuc.pushlog;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class ReminderNotifier {
    private static final String CHANNEL_ID = "daily_pushup_reminder";
    private static final int NOTIFICATION_ID = 2402;

    private ReminderNotifier() {}

    static void ensureChannel(Context context) {
        NotificationManager notifications =
                context.getSystemService(NotificationManager.class);
        if (notifications == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Daily push-up reminder",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Reminds you only when today's push-up total is below 10");
        notifications.createNotificationChannel(channel);
    }

    static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager notifications =
                context.getSystemService(NotificationManager.class);
        return notifications != null && notifications.areNotificationsEnabled();
    }

    static void show(Context context, int todayCount) {
        ensureChannel(context);
        if (!canPost(context)) {
            return;
        }
        int remaining = DailyReminderLogic.remainingPushups(todayCount);
        String message = todayCount == 0
                ? "You haven’t logged any today. Start with 10."
                : "You’re at " + todayCount + " today—" + remaining +
                        " more and today’s reminder is done.";

        Intent openApp = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Time for push-ups")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build();

        NotificationManager notifications =
                context.getSystemService(NotificationManager.class);
        if (notifications != null) {
            notifications.notify(NOTIFICATION_ID, notification);
        }
    }

    static void cancel(Context context) {
        NotificationManager notifications =
                context.getSystemService(NotificationManager.class);
        if (notifications != null) {
            notifications.cancel(NOTIFICATION_ID);
        }
    }
}
