package com.thuc.pushlog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.LocalTime;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ReminderScheduler.ACTION_DAILY_REMINDER.equals(intent.getAction()) ||
                !ReminderPreferences.isEnabled(context)) {
            return;
        }

        // Schedule tomorrow first so a notification failure cannot break the series.
        ReminderScheduler.scheduleNext(context);
        if (!DailyReminderLogic.isWithinDeliveryWindow(
                ReminderPreferences.hour(context),
                ReminderPreferences.minute(context),
                LocalTime.now())) {
            return;
        }
        evaluateToday(context);
    }

    static void evaluateToday(Context context) {
        if (!ReminderPreferences.isEnabled(context)) {
            return;
        }
        LocalDate today = LocalDate.now();
        boolean handled = ReminderPreferences.wasHandledOn(context, today);
        try (PushupDatabase database = new PushupDatabase(context.getApplicationContext())) {
            int todayCount = database.getCount(today);
            if (DailyReminderLogic.shouldNotify(todayCount, handled)) {
                ReminderNotifier.show(context, todayCount);
            } else if (todayCount >= DailyReminderLogic.COMPLETION_THRESHOLD) {
                ReminderNotifier.cancel(context);
            }
        }
        ReminderPreferences.markHandled(context, today);
    }
}
