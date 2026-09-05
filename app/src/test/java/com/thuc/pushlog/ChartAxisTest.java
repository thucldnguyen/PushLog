package com.thuc.pushlog;

public final class ChartAxisTest {
    public static void main(String[] args) {
        assertScale(0, 40, 20, 2);
        assertScale(40, 40, 20, 2);
        assertScale(120, 120, 20, 6);
        assertScale(121, 140, 20, 7);
        assertScale(240, 240, 20, 12);
        assertScale(241, 280, 40, 7);

        for (int maximum = 0; maximum <= 999_999; maximum++) {
            ChartAxis axis = ChartAxis.forMaximum(maximum);
            if (axis.maximum < maximum || axis.maximum % 20 != 0 ||
                    axis.interval % 20 != 0 || axis.steps > 12) {
                throw new AssertionError("Invalid axis for maximum " + maximum);
            }
        }
    }

    private static void assertScale(int input, int maximum, int interval, int steps) {
        ChartAxis actual = ChartAxis.forMaximum(input);
        if (actual.maximum != maximum || actual.interval != interval || actual.steps != steps) {
            throw new AssertionError("Unexpected axis for " + input +
                    ": maximum=" + actual.maximum +
                    ", interval=" + actual.interval +
                    ", steps=" + actual.steps);
        }
    }
}
