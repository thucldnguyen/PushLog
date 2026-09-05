package com.thuc.pushlog;

final class ChartAxis {
    private static final int PUSHUPS_PER_SET = 20;
    private static final int MAX_GRID_STEPS = 12;

    final int maximum;
    final int interval;
    final int steps;

    private ChartAxis(int maximum, int interval) {
        this.maximum = maximum;
        this.interval = interval;
        this.steps = maximum / interval;
    }

    static ChartAxis forMaximum(int dailyMaximum) {
        int safeMaximum = Math.max(0, dailyMaximum);
        int minimumInterval = divideRoundingUp(safeMaximum, MAX_GRID_STEPS);
        int interval = Math.max(PUSHUPS_PER_SET,
                roundUp(minimumInterval, PUSHUPS_PER_SET));
        int maximum = safeMaximum == 0
                ? PUSHUPS_PER_SET * 2
                : roundUp(safeMaximum, interval);
        return new ChartAxis(maximum, interval);
    }

    private static int divideRoundingUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static int roundUp(int value, int interval) {
        return divideRoundingUp(value, interval) * interval;
    }
}
