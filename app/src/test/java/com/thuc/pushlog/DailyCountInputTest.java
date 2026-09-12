package com.thuc.pushlog;

public final class DailyCountInputTest {
    public static void main(String[] args) {
        assertCount("0", 0);
        assertCount("40", 40);
        assertCount(" 125 ", 125);
        assertCount("999999", 999_999);
        assertInvalid("");
        assertInvalid("ten");
        assertInvalid("-1");
        assertInvalid("1000000");
    }

    private static void assertCount(String input, int expected) {
        int actual = DailyCountInput.parse(input);
        if (actual != expected) {
            throw new AssertionError("Unexpected count for " + input + ": " + actual);
        }
    }

    private static void assertInvalid(String input) {
        try {
            DailyCountInput.parse(input);
            throw new AssertionError("Expected invalid count: " + input);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
