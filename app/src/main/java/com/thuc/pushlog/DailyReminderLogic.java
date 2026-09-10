package com.thuc.pushlog;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

final class DailyReminderLogic {
    static final int COMPLETION_THRESHOLD = 10;
    static final int DEFAULT_REMINDER_HOUR = 22;
    static final int DEFAULT_REMINDER_MINUTE = 0;
    static final int MAX_DELIVERY_DELAY_MINUTES = 90;

    private DailyReminderLogic() {}

    static boolean shouldNotify(int todayCount, boolean alreadyHandledToday) {
        return todayCount < COMPLETION_THRESHOLD && !alreadyHandledToday;
    }

    static int remainingPushups(int todayCount) {
        return Math.max(0, COMPLETION_THRESHOLD - todayCount);
    }

    static boolean isWithinDeliveryWindow(int hour, int minute, LocalTime now) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Reminder time is invalid");
        }
        int scheduledMinute = hour * 60 + minute;
        int currentMinute = now.getHour() * 60 + now.getMinute();
        int minutesLate = Math.floorMod(currentMinute - scheduledMinute, 24 * 60);
        return minutesLate <= MAX_DELIVERY_DELAY_MINUTES;
    }

    static long nextTriggerMillis(int hour, int minute, ZoneId zone, Instant now) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Reminder time is invalid");
        }
        ZonedDateTime localNow = now.atZone(zone);
        ZonedDateTime next = localNow
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);
        if (!next.toInstant().isAfter(now)) {
            next = next.plusDays(1);
        }
        return next.toInstant().toEpochMilli();
    }
}
