package com.thuc.pushlog;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class HistoryActivity extends Activity {
    private static final String PREFS = "com.thuc.pushlog.history_preferences";
    private static final String PREF_BAR_CHART = "show_bar_chart";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicInteger renderGeneration = new AtomicInteger();
    private PushupDatabase database;
    private YearMonth selectedMonth = YearMonth.now();
    private TextView monthTitle;
    private TextView viewToggle;
    private LinearLayout monthBody;
    private boolean showBarChart;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        showBarChart = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_BAR_CHART, false);
        database = new PushupDatabase(getApplicationContext());
        setContentView(buildScreen());
        renderMonth();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        applySystemBarInsets(scroll);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 28));
        scroll.addView(page, Ui.matchWrap());

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top, Ui.matchWrap());

        TextView back = Ui.iconButton(this, "←", 28);
        back.setContentDescription("Back");
        back.setOnClickListener(view -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 52)));

        TextView heading = Ui.title(this, "Monthly history", 24);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        headingParams.leftMargin = Ui.dp(this, 14);
        top.addView(heading, headingParams);

        TextView subtitle = Ui.text(this, "Every day, including the quiet ones.", 14, Ui.MUTED);
        LinearLayout.LayoutParams subtitleParams = Ui.matchWrap();
        subtitleParams.topMargin = Ui.dp(this, 14);
        page.addView(subtitle, subtitleParams);

        LinearLayout navigator = new LinearLayout(this);
        navigator.setOrientation(LinearLayout.HORIZONTAL);
        navigator.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams navigatorParams = Ui.matchWrap();
        navigatorParams.topMargin = Ui.dp(this, 22);
        page.addView(navigator, navigatorParams);

        TextView previous = Ui.iconButton(this, "‹", 32);
        previous.setContentDescription("Previous month");
        previous.setOnClickListener(view -> {
            selectedMonth = selectedMonth.minusMonths(1);
            renderMonth();
        });
        navigator.addView(previous, new LinearLayout.LayoutParams(Ui.dp(this, 50), Ui.dp(this, 50)));

        monthTitle = Ui.title(this, "", 20);
        monthTitle.setGravity(Gravity.CENTER);
        navigator.addView(monthTitle, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView next = Ui.iconButton(this, "›", 32);
        next.setContentDescription("Next month");
        next.setOnClickListener(view -> {
            selectedMonth = selectedMonth.plusMonths(1);
            renderMonth();
        });
        navigator.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 50), Ui.dp(this, 50)));

        viewToggle = Ui.action(this, "", false);
        viewToggle.setTextColor(Ui.GOLD);
        viewToggle.setOnClickListener(view -> {
            showBarChart = !showBarChart;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(PREF_BAR_CHART, showBarChart)
                    .apply();
            updateToggleLabel();
            renderMonth();
        });
        updateToggleLabel();
        LinearLayout.LayoutParams toggleParams = Ui.matchWrap();
        toggleParams.topMargin = Ui.dp(this, 12);
        page.addView(viewToggle, toggleParams);

        monthBody = new LinearLayout(this);
        monthBody.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyParams = Ui.matchWrap();
        bodyParams.topMargin = Ui.dp(this, 14);
        page.addView(monthBody, bodyParams);
        return scroll;
    }

    private void updateToggleLabel() {
        if (viewToggle == null) {
            return;
        }
        viewToggle.setText(showBarChart ? "View as calendar" : "View as bar chart");
        viewToggle.setContentDescription(showBarChart
                ? "Switch monthly history to calendar view"
                : "Switch monthly history to bar chart view");
    }

    private void applySystemBarInsets(View root) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }
    }

    private void renderMonth() {
        final int generation = renderGeneration.incrementAndGet();
        final YearMonth month = selectedMonth;
        String label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
        monthTitle.setText(label);
        monthBody.removeAllViews();
        TextView loading = Ui.text(this, "Loading…", 14, Ui.MUTED);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, Ui.dp(this, 32), 0, Ui.dp(this, 32));
        monthBody.addView(loading, Ui.matchWrap());

        io.execute(() -> {
            PushupDatabase.MonthData data = database.getMonth(month);
            runOnUiThread(() -> {
                if (generation != renderGeneration.get() || isFinishing() || isDestroyed()) {
                    return;
                }
                drawMonth(month, data);
            });
        });
    }

    private void drawMonth(YearMonth month, PushupDatabase.MonthData data) {
        monthBody.removeAllViews();
        monthBody.addView(buildSummary(data), Ui.matchWrap());

        if (showBarChart) {
            addBarChart(month, data);
        } else {
            addCalendar(month, data);
        }
    }

    private void addBarChart(YearMonth month, PushupDatabase.MonthData data) {
        LinearLayout chartCard = new LinearLayout(this);
        chartCard.setOrientation(LinearLayout.VERTICAL);
        chartCard.setPadding(Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 8));
        chartCard.setBackground(Ui.rounded(Ui.CARD, 20, this));
        LinearLayout.LayoutParams cardParams = Ui.matchWrap();
        cardParams.topMargin = Ui.dp(this, 16);
        monthBody.addView(chartCard, cardParams);

        TextView title = Ui.title(this, "Daily push-ups", 16);
        LinearLayout.LayoutParams titleParams = Ui.matchWrap();
        titleParams.leftMargin = Ui.dp(this, 8);
        chartCard.addView(title, titleParams);

        TextView subtitle = Ui.text(this, "Every bar starts at zero", 12, Ui.MUTED);
        LinearLayout.LayoutParams subtitleParams = Ui.matchWrap();
        subtitleParams.leftMargin = Ui.dp(this, 8);
        subtitleParams.topMargin = Ui.dp(this, 2);
        chartCard.addView(subtitle, subtitleParams);

        MonthlyBarChartView chart = new MonthlyBarChartView(this, month, data);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 270));
        chartParams.topMargin = Ui.dp(this, 4);
        chartCard.addView(chart, chartParams);

        TextView legend = Ui.text(this, "Horizontal axis: day  •  Vertical axis: push-ups", 12, Ui.MUTED);
        legend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legendParams = Ui.matchWrap();
        legendParams.topMargin = Ui.dp(this, 12);
        monthBody.addView(legend, legendParams);
    }

    private void addCalendar(YearMonth month, PushupDatabase.MonthData data) {

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        LinearLayout.LayoutParams gridParams = Ui.matchWrap();
        gridParams.topMargin = Ui.dp(this, 16);
        monthBody.addView(grid, gridParams);

        String[] weekdayLabels = {"M", "T", "W", "T", "F", "S", "S"};
        String[] weekdayDescriptions = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < weekdayLabels.length; i++) {
            TextView label = Ui.title(this, weekdayLabels[i], 11);
            label.setTextColor(Ui.MUTED);
            label.setGravity(Gravity.CENTER);
            label.setContentDescription(weekdayDescriptions[i]);
            grid.addView(label, gridParams(Ui.dp(this, 30), Ui.dp(this, 2)));
        }

        int leadingBlankDays = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leadingBlankDays; i++) {
            grid.addView(new View(this), gridParams(Ui.dp(this, 66), Ui.dp(this, 2)));
        }

        LocalDate today = LocalDate.now();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            int count = data.days.getOrDefault(date, 0);
            grid.addView(buildDayCell(date, count, data.best, date.equals(today)),
                    gridParams(Ui.dp(this, 66), Ui.dp(this, 2)));
        }

        TextView legend = Ui.text(this, "Top number: date  •  Bottom number: push-ups", 12, Ui.MUTED);
        legend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legendParams = Ui.matchWrap();
        legendParams.topMargin = Ui.dp(this, 14);
        monthBody.addView(legend, legendParams);
    }

    private View buildSummary(PushupDatabase.MonthData data) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16));
        card.setBackground(Ui.rounded(Ui.CARD, 20, this));
        addSummaryStat(card, NumberFormat.getIntegerInstance().format(data.total), "MONTH TOTAL");
        addSummaryStat(card, NumberFormat.getIntegerInstance().format(data.best), "BEST DAY");
        addSummaryStat(card, NumberFormat.getIntegerInstance().format(data.activeDays), "ACTIVE DAYS");
        return card;
    }

    private void addSummaryStat(LinearLayout parent, String value, String label) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER);
        TextView number = Ui.title(this, value, 20);
        number.setGravity(Gravity.CENTER);
        block.addView(number);
        TextView caption = Ui.text(this, label, 9, Ui.MUTED);
        caption.setLetterSpacing(0.06f);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams = Ui.matchWrap();
        captionParams.topMargin = Ui.dp(this, 3);
        block.addView(caption, captionParams);
        parent.addView(block, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    }

    private View buildDayCell(LocalDate date, int count, int monthBest, boolean isToday) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6));

        int fill = Ui.CARD;
        if (count > 0) {
            float strength = monthBest == 0 ? 0 : count / (float) monthBest;
            int red = 45 + Math.round(48 * strength);
            int green = 39 + Math.round(35 * strength);
            int blue = 25 + Math.round(6 * strength);
            fill = Color.rgb(red, green, blue);
        }
        cell.setBackground(isToday
                ? Ui.outlined(fill, Ui.GOLD, 13, this)
                : Ui.rounded(fill, 13, this));

        TextView day = Ui.title(this, Integer.toString(date.getDayOfMonth()), 13);
        day.setGravity(Gravity.CENTER);
        day.setTextColor(isToday ? Ui.GOLD : Ui.TEXT);
        cell.addView(day);

        TextView value = Ui.text(this, count == 0 ? "—" : NumberFormat.getIntegerInstance().format(count), 13,
                count == 0 ? Ui.MUTED : Ui.TEXT);
        value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        value.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams valueParams = Ui.matchWrap();
        valueParams.topMargin = Ui.dp(this, 4);
        cell.addView(value, valueParams);
        cell.setContentDescription(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())) +
                ": " + count + " push-ups");
        return cell;
    }

    private GridLayout.LayoutParams gridParams(int height, int margin) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = height;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(margin, margin, margin, margin);
        return params;
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }
}
