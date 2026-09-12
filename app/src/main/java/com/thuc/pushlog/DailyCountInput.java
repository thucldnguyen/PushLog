package com.thuc.pushlog;

final class DailyCountInput {
    static final int MAX_COUNT = 999_999;

    private DailyCountInput() {}

    static int parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a push-up total");
        }
        final long count;
        try {
            count = Long.parseLong(value.trim());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Enter a whole number", invalid);
        }
        if (count < 0 || count > MAX_COUNT) {
            throw new IllegalArgumentException("Enter a total from 0 to 999,999");
        }
        return (int) count;
    }
}
