package com.thuc.pushlog;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public final class DailyReminderLogicTest {
    public static void main(String[] args) {
        assertDecision(0, false, true);
        assertDecision(9, false, true);
        assertDecision(10, false, false);
        assertDecision(40, false, false);
        assertDecision(0, true, false);

        if (DailyReminderLogic.remainingPushups(0) != 10 ||
                DailyReminderLogic.remainingPushups(9) != 1 ||
                DailyReminderLogic.remainingPushups(20) != 0) {
            throw new AssertionError("Remaining push-up calculation is incorrect");
        }
        if (!DailyReminderLogic.DEFAULT_REMINDER_ENABLED ||
                DailyReminderLogic.DEFAULT_REMINDER_HOUR != 22 ||
                DailyReminderLogic.DEFAULT_REMINDER_MINUTE != 0) {
            throw new AssertionError("The default reminder must be enabled at 10:00 PM");
        }
        if (DailyReminderLogic.isWithinDeliveryWindow(22, 0, LocalTime.of(21, 59)) ||
                !DailyReminderLogic.isWithinDeliveryWindow(22, 0, LocalTime.of(22, 0)) ||
                !DailyReminderLogic.isWithinDeliveryWindow(22, 0, LocalTime.of(23, 30)) ||
                DailyReminderLogic.isWithinDeliveryWindow(22, 0, LocalTime.of(23, 31)) ||
                DailyReminderLogic.isWithinDeliveryWindow(22, 0, LocalTime.of(3, 0))) {
            throw new AssertionError("Reminder delivery window is incorrect");
        }

        ZoneId vietnam = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant before = Instant.parse("2026-09-04T10:00:00Z");
        Instant atTime = Instant.parse("2026-09-04T15:00:00Z");
        long sameDay = DailyReminderLogic.nextTriggerMillis(22, 0, vietnam, before);
        long nextDay = DailyReminderLogic.nextTriggerMillis(22, 0, vietnam, atTime);
        if (sameDay != Instant.parse("2026-09-04T15:00:00Z").toEpochMilli() ||
                nextDay != Instant.parse("2026-09-05T15:00:00Z").toEpochMilli()) {
            throw new AssertionError("Daily reminder scheduling is incorrect");
        }

        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                long trigger = DailyReminderLogic.nextTriggerMillis(
                        hour, minute, vietnam, before);
                if (trigger <= before.toEpochMilli()) {
                    throw new AssertionError("Reminder was not scheduled in the future");
                }
            }
        }
    }

    private static void assertDecision(int count, boolean handled, boolean expected) {
        boolean actual = DailyReminderLogic.shouldNotify(count, handled);
        if (actual != expected) {
            throw new AssertionError("Unexpected reminder decision for count " + count);
        }
    }
}
