package com.thuc.pushlog;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

final class ReminderPreferences {
    private static final String PREFERENCES = "pushlog_daily_reminder";
    private static final String ENABLED = "enabled";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String LAST_HANDLED_DATE = "last_handled_date";
    private static final String DEFAULT_TIME_MIGRATED = "default_time_migrated_to_22";
    private static final int LEGACY_DEFAULT_HOUR = 20;

    private ReminderPreferences() {}

    static boolean isEnabled(Context context) {
        return preferences(context).getBoolean(ENABLED, false);
    }

    static int hour(Context context) {
        return preferences(context).getInt(HOUR, DailyReminderLogic.DEFAULT_REMINDER_HOUR);
    }

    static int minute(Context context) {
        return preferences(context).getInt(MINUTE, DailyReminderLogic.DEFAULT_REMINDER_MINUTE);
    }

    static void migrateLegacyDefault(Context context) {
        SharedPreferences preferences = preferences(context);
        if (preferences.getBoolean(DEFAULT_TIME_MIGRATED, false)) {
            return;
        }

        SharedPreferences.Editor migration = preferences.edit()
                .putBoolean(DEFAULT_TIME_MIGRATED, true);
        if (preferences.getInt(HOUR, DailyReminderLogic.DEFAULT_REMINDER_HOUR) ==
                LEGACY_DEFAULT_HOUR &&
                preferences.getInt(MINUTE, DailyReminderLogic.DEFAULT_REMINDER_MINUTE) == 0) {
            migration.putInt(HOUR, DailyReminderLogic.DEFAULT_REMINDER_HOUR);
        }
        migration.apply();
    }

    static void enable(Context context, int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Reminder time is invalid");
        }
        preferences(context).edit()
                .putBoolean(ENABLED, true)
                .putInt(HOUR, hour)
                .putInt(MINUTE, minute)
                .apply();
    }

    static void disable(Context context) {
        preferences(context).edit().putBoolean(ENABLED, false).apply();
    }

    static boolean wasHandledOn(Context context, LocalDate date) {
        return date.toString().equals(
                preferences(context).getString(LAST_HANDLED_DATE, null));
    }

    static void markHandled(Context context, LocalDate date) {
        preferences(context).edit()
                .putString(LAST_HANDLED_DATE, date.toString())
                .commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
