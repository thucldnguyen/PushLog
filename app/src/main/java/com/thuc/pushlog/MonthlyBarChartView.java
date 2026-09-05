package com.thuc.pushlog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

final class MonthlyBarChartView extends View {
    private final YearMonth month;
    private final Map<LocalDate, Integer> dailyCounts;
    private final int yMaximum;
    private final int yInterval;
    private final int gridSteps;
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private float plotLeft;
    private float plotTop;
    private float plotRight;
    private float plotBottom;

    MonthlyBarChartView(Context context, YearMonth month, PushupDatabase.MonthData data) {
        super(context);
        this.month = month;
        this.dailyCounts = data.days;
        ChartAxis axis = ChartAxis.forMaximum(data.best);
        this.yMaximum = axis.maximum;
        this.yInterval = axis.interval;
        this.gridSteps = axis.steps;

        gridPaint.setColor(Ui.LINE);
        gridPaint.setStrokeWidth(Ui.dp(context, 1));

        textPaint.setColor(Ui.MUTED);
        textPaint.setTextSize(sp(context, 10));
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        barPaint.setColor(Ui.GOLD);
        todayBarPaint.setColor(Color.rgb(255, 218, 104));

        String monthName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
        setContentDescription("Daily push-up bar chart for " + monthName +
                ". Month total " + data.total + ", best day " + data.best +
                ". Switch to calendar view for exact daily values.");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = Ui.dp(getContext(), 270);
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        plotLeft = getPaddingLeft() + Ui.dp(getContext(), 38);
        plotTop = getPaddingTop() + Ui.dp(getContext(), 14);
        plotRight = width - getPaddingRight() - Ui.dp(getContext(), 8);
        plotBottom = height - getPaddingBottom() - Ui.dp(getContext(), 30);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (plotRight <= plotLeft || plotBottom <= plotTop) {
            return;
        }

        float plotHeight = plotBottom - plotTop;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.getDefault());
        for (int step = 0; step <= gridSteps; step++) {
            float ratio = step / (float) gridSteps;
            float y = plotBottom - (ratio * plotHeight);
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
            int label = step * yInterval;
            canvas.drawText(numbers.format(label), plotLeft - Ui.dp(getContext(), 7),
                    y + textPaint.getTextSize() * 0.35f, textPaint);
        }

        int daysInMonth = month.lengthOfMonth();
        float slotWidth = (plotRight - plotLeft) / daysInMonth;
        float barWidth = Math.max(Ui.dp(getContext(), 3), slotWidth * 0.58f);
        float radius = Math.min(Ui.dp(getContext(), 3), barWidth / 2f);
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = month.atDay(day);
            int count = dailyCounts.getOrDefault(date, 0);
            float centerX = plotLeft + ((day - 0.5f) * slotWidth);
            if (count > 0) {
                float height = (count / (float) yMaximum) * plotHeight;
                height = Math.max(height, Ui.dp(getContext(), 2));
                bar.set(centerX - barWidth / 2f, plotBottom - height,
                        centerX + barWidth / 2f, plotBottom);
                canvas.drawRoundRect(bar, radius, radius, date.equals(today) ? todayBarPaint : barPaint);
            }

            boolean labelDay = day == 1 || day == daysInMonth ||
                    (day % 5 == 0 && day <= daysInMonth - 3);
            if (labelDay) {
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(Integer.toString(day), centerX,
                        plotBottom + Ui.dp(getContext(), 19), textPaint);
            }
        }
    }

    private static float sp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().scaledDensity;
    }
}
