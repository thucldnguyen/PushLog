package com.thuc.pushlog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalTime;

public final class ReminderRescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ReminderPreferences.isEnabled(context)) {
            ReminderScheduler.scheduleNext(context);
            if (DailyReminderLogic.isDueToday(
                    ReminderPreferences.hour(context),
                    ReminderPreferences.minute(context),
                    LocalTime.now())) {
                ReminderReceiver.evaluateToday(context);
            }
        }
    }
}
