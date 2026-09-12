package com.thuc.pushlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final String STATE_SELECTED_MONTH = "selected_month";
    private static final int VISUALIZATION_HEIGHT_DP = 438;
    private static final int CALENDAR_CELL_HEIGHT_DP = 58;
    private static final int CALENDAR_DAY_SLOTS = 42;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicInteger renderGeneration = new AtomicInteger();
    private PushupDatabase database;
    private YearMonth selectedMonth = YearMonth.now();
    private TextView monthTitle;
    private TextView calendarTab;
    private TextView chartTab;
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
        if (state != null && state.containsKey(STATE_SELECTED_MONTH)) {
            selectedMonth = YearMonth.parse(state.getString(STATE_SELECTED_MONTH));
        }
        database = new PushupDatabase(getApplicationContext());
        setContentView(buildScreen());
        renderMonth();
    }

    private View buildScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Ui.BG);
        applySystemBarInsets(screen);

        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        screen.addView(appBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 64)));

        ImageButton back = Ui.toolbarButton(this, R.drawable.ic_arrow_back, "Back");
        back.setOnClickListener(view -> finish());
        appBar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        TextView heading = Ui.title(this, "History", 21);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        headingParams.leftMargin = Ui.dp(this, 12);
        appBar.addView(heading, headingParams);

        ImageButton editDate = Ui.toolbarButton(
                this, R.drawable.ic_edit_calendar, "Choose a date to edit");
        editDate.setOnClickListener(view -> showDatePicker());
        appBar.addView(editDate, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        screen.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 18));
        scroll.addView(page, Ui.matchWrap());

        LinearLayout navigator = new LinearLayout(this);
        navigator.setOrientation(LinearLayout.HORIZONTAL);
        navigator.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(navigator, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

        ImageButton previous = Ui.toolbarButton(
                this, R.drawable.ic_chevron_left, "Previous month");
        previous.setContentDescription("Previous month");
        previous.setOnClickListener(view -> {
            selectedMonth = selectedMonth.minusMonths(1);
            renderMonth();
        });
        navigator.addView(previous, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        monthTitle = Ui.title(this, "", 20);
        monthTitle.setGravity(Gravity.CENTER);
        navigator.addView(monthTitle, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton next = Ui.toolbarButton(
                this, R.drawable.ic_chevron_right, "Next month");
        next.setContentDescription("Next month");
        next.setOnClickListener(view -> {
            selectedMonth = selectedMonth.plusMonths(1);
            renderMonth();
        });
        navigator.addView(next, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));

        LinearLayout viewSelector = new LinearLayout(this);
        viewSelector.setOrientation(LinearLayout.HORIZONTAL);
        viewSelector.setPadding(Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2), Ui.dp(this, 2));
        viewSelector.setBackground(Ui.rounded(Ui.CARD, 16, this));
        calendarTab = Ui.segment(this, "Calendar", !showBarChart);
        calendarTab.setContentDescription("Show calendar view");
        calendarTab.setOnClickListener(view -> setBarChartVisible(false));
        viewSelector.addView(calendarTab, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 48), 1f));
        chartTab = Ui.segment(this, "Chart", showBarChart);
        chartTab.setContentDescription("Show bar chart view");
        chartTab.setOnClickListener(view -> setBarChartVisible(true));
        LinearLayout.LayoutParams chartTabParams = new LinearLayout.LayoutParams(
                0, Ui.dp(this, 48), 1f);
        chartTabParams.leftMargin = Ui.dp(this, 2);
        viewSelector.addView(chartTab, chartTabParams);
        page.addView(viewSelector, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

        monthBody = new LinearLayout(this);
        monthBody.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyParams = Ui.matchWrap();
        bodyParams.topMargin = Ui.dp(this, 10);
        page.addView(monthBody, bodyParams);
        return screen;
    }

    private void setBarChartVisible(boolean visible) {
        if (showBarChart == visible) {
            return;
        }
        showBarChart = visible;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(PREF_BAR_CHART, showBarChart)
                .apply();
        updateViewSelector();
        renderMonth();
    }

    private void updateViewSelector() {
        if (calendarTab == null || chartTab == null) {
            return;
        }
        calendarTab.setTextColor(showBarChart ? Ui.MUTED : Ui.GOLD);
        calendarTab.setBackground(showBarChart
                ? Ui.ripple(this, Ui.CARD, 14, Color.argb(45, 255, 255, 255))
                : Ui.outlinedRipple(this, Ui.GOLD_WASH, Ui.GOLD_DARK, 14));
        calendarTab.setSelected(!showBarChart);
        chartTab.setTextColor(showBarChart ? Ui.GOLD : Ui.MUTED);
        chartTab.setBackground(showBarChart
                ? Ui.outlinedRipple(this, Ui.GOLD_WASH, Ui.GOLD_DARK, 14)
                : Ui.ripple(this, Ui.CARD, 14, Color.argb(45, 255, 255, 255)));
        chartTab.setSelected(showBarChart);
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

        LinearLayout visualization = new LinearLayout(this);
        visualization.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams visualizationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, VISUALIZATION_HEIGHT_DP));
        visualizationParams.topMargin = Ui.dp(this, 10);
        monthBody.addView(visualization, visualizationParams);

        if (showBarChart) {
            addBarChart(month, data, visualization);
        } else {
            addCalendar(month, data, visualization);
        }
    }

    private void addBarChart(YearMonth month, PushupDatabase.MonthData data,
                             LinearLayout visualization) {
        LinearLayout chartCard = new LinearLayout(this);
        chartCard.setOrientation(LinearLayout.VERTICAL);
        chartCard.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 6));
        chartCard.setBackground(Ui.rounded(Ui.CARD, 20, this));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        cardParams.bottomMargin = Ui.dp(this, 8);
        visualization.addView(chartCard, cardParams);

        TextView title = Ui.title(this, "Daily push-ups", 16);
        LinearLayout.LayoutParams titleParams = Ui.matchWrap();
        titleParams.leftMargin = Ui.dp(this, 8);
        chartCard.addView(title, titleParams);

        MonthlyBarChartView chart = new MonthlyBarChartView(this, month, data);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        chartParams.topMargin = Ui.dp(this, 2);
        chartCard.addView(chart, chartParams);

        TextView legend = Ui.text(this, "Horizontal axis: day  •  Vertical axis: push-ups", 12, Ui.MUTED);
        legend.setGravity(Gravity.CENTER);
        visualization.addView(legend, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 36)));
    }

    private void addCalendar(YearMonth month, PushupDatabase.MonthData data,
                             LinearLayout visualization) {

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        grid.setRowCount(7);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        grid.setPadding(0, Ui.dp(this, 4), 0, 0);
        visualization.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        String[] weekdayLabels = {"M", "T", "W", "T", "F", "S", "S"};
        String[] weekdayDescriptions = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < weekdayLabels.length; i++) {
            TextView label = Ui.title(this, weekdayLabels[i], 11);
            label.setTextColor(Ui.MUTED);
            label.setGravity(Gravity.CENTER);
            label.setContentDescription(weekdayDescriptions[i]);
            grid.addView(label, gridParams(Ui.dp(this, 28), Ui.dp(this, 1)));
        }

        int leadingBlankDays = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < leadingBlankDays; i++) {
            grid.addView(new View(this), gridParams(
                    Ui.dp(this, CALENDAR_CELL_HEIGHT_DP), Ui.dp(this, 2)));
        }

        LocalDate today = LocalDate.now();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            int count = data.days.getOrDefault(date, 0);
            grid.addView(buildDayCell(date, count, data.best, date.equals(today)),
                    gridParams(Ui.dp(this, CALENDAR_CELL_HEIGHT_DP), Ui.dp(this, 2)));
        }

        int usedSlots = leadingBlankDays + month.lengthOfMonth();
        for (int i = usedSlots; i < CALENDAR_DAY_SLOTS; i++) {
            grid.addView(new View(this), gridParams(
                    Ui.dp(this, CALENDAR_CELL_HEIGHT_DP), Ui.dp(this, 2)));
        }

        TextView legend = Ui.text(this, "Tap a day to edit its push-up total", 12, Ui.MUTED);
        legend.setGravity(Gravity.CENTER);
        visualization.addView(legend, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 36)));
    }

    private View buildSummary(PushupDatabase.MonthData data) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(Ui.dp(this, 6), Ui.dp(this, 11), Ui.dp(this, 6), Ui.dp(this, 11));
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
        TextView number = Ui.title(this, value, 19);
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
        cell.setPadding(Ui.dp(this, 1), Ui.dp(this, 4), Ui.dp(this, 1), Ui.dp(this, 4));

        int fill = Ui.CARD;
        if (count > 0) {
            float strength = monthBest == 0 ? 0 : count / (float) monthBest;
            int red = 45 + Math.round(48 * strength);
            int green = 39 + Math.round(35 * strength);
            int blue = 25 + Math.round(6 * strength);
            fill = Color.rgb(red, green, blue);
        }
        cell.setBackground(isToday
                ? Ui.outlined(fill, Ui.GOLD, 10, this)
                : Ui.rounded(fill, 10, this));

        TextView day = Ui.title(this, Integer.toString(date.getDayOfMonth()), 13);
        day.setGravity(Gravity.CENTER);
        day.setTextColor(isToday ? Ui.GOLD : Ui.TEXT);
        cell.addView(day);

        TextView value = Ui.text(this, count == 0 ? "—" : NumberFormat.getIntegerInstance().format(count), 13,
                count == 0 ? Ui.MUTED : Ui.TEXT);
        value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        value.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams valueParams = Ui.matchWrap();
        valueParams.topMargin = Ui.dp(this, 1);
        cell.addView(value, valueParams);
        cell.setContentDescription(date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())) +
                ": " + count + " push-ups" +
                (date.isAfter(LocalDate.now()) ? "" : ". Tap to edit"));
        if (!date.isAfter(LocalDate.now())) {
            cell.setClickable(true);
            cell.setFocusable(true);
            cell.setOnClickListener(view -> showEditDialog(date, count));
        } else {
            cell.setAlpha(0.55f);
        }
        return cell;
    }

    private void showDatePicker() {
        LocalDate today = LocalDate.now();
        LocalDate initial = selectedMonth.isAfter(YearMonth.from(today))
                ? today
                : selectedMonth.atDay(Math.min(today.getDayOfMonth(), selectedMonth.lengthOfMonth()));
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, day) -> loadAndShowEditDialog(
                        LocalDate.of(year, month + 1, day)),
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth());
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void loadAndShowEditDialog(LocalDate date) {
        io.execute(() -> {
            int count = database.getCount(date);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    showEditDialog(date, count);
                }
            });
        });
    }

    private void showEditDialog(LocalDate date, int currentCount) {
        if (date.isAfter(LocalDate.now())) {
            return;
        }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(Integer.toString(currentCount));
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setContentDescription("Push-up total for " + date);
        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setPadding(Ui.dp(this, 24), Ui.dp(this, 4),
                Ui.dp(this, 24), Ui.dp(this, 4));
        inputContainer.addView(input, Ui.matchWrap());

        String title = date.format(DateTimeFormatter.ofPattern(
                "EEEE, MMMM d", Locale.getDefault()));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Enter the total push-ups completed on this date. Use 0 to clear it.")
                .setView(inputContainer)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.GOLD);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                final int count;
                try {
                    count = DailyCountInput.parse(input.getText().toString());
                } catch (IllegalArgumentException invalid) {
                    input.setError(invalid.getMessage());
                    return;
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                saveDate(date, count, dialog);
            });
        });
        dialog.show();
    }

    private void saveDate(LocalDate date, int count, AlertDialog dialog) {
        io.execute(() -> {
            try {
                database.setCount(date, count);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (date.equals(LocalDate.now()) &&
                            count >= DailyReminderLogic.COMPLETION_THRESHOLD) {
                        ReminderNotifier.cancel(this);
                    }
                    dialog.dismiss();
                    selectedMonth = YearMonth.from(date);
                    renderMonth();
                    Toast.makeText(this, "Push-up total updated", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    showSaveError(failure.getMessage());
                });
            }
        });
    }

    private void showSaveError(String message) {
        Toast.makeText(this,
                message == null ? "The total could not be saved" : message,
                Toast.LENGTH_LONG).show();
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
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_SELECTED_MONTH, selectedMonth.toString());
        super.onSaveInstanceState(state);
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
