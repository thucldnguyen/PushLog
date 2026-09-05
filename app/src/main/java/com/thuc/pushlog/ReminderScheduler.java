package com.thuc.pushlog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.Instant;
import java.time.ZoneId;

final class ReminderScheduler {
    static final String ACTION_DAILY_REMINDER = "com.thuc.pushlog.DAILY_REMINDER";
    private static final int REQUEST_CODE = 2401;

    private ReminderScheduler() {}

    static void scheduleNext(Context context) {
        if (!ReminderPreferences.isEnabled(context)) {
            cancel(context);
            return;
        }
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) {
            return;
        }
        long triggerAt = DailyReminderLogic.nextTriggerMillis(
                ReminderPreferences.hour(context),
                ReminderPreferences.minute(context),
                ZoneId.systemDefault(),
                Instant.now());
        alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context));
    }

    static void cancel(Context context) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms != null) {
            alarms.cancel(pendingIntent(context));
        }
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ACTION_DAILY_REMINDER);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
