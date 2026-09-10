package com.thuc.pushlog;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_IMPORT = 1001;
    private static final int REQUEST_NOTIFICATIONS = 1002;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private PushupDatabase database;
    private TextView totalValue;
    private TextView bestValue;
    private TextView averageValue;
    private TextView todayValue;
    private TextView todayDate;
    private TextView reminderButton;
    private int pendingReminderHour = -1;
    private int pendingReminderMinute = -1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        database = new PushupDatabase(getApplicationContext());
        ReminderNotifier.ensureChannel(this);
        setContentView(buildHome());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) {
            refreshDashboard();
        }
        if (pendingReminderHour >= 0 && ReminderNotifier.canPost(this)) {
            enablePendingReminder();
        }
        updateReminderButton();
        if (ReminderPreferences.isEnabled(this)) {
            ReminderScheduler.scheduleNext(this);
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Ui.BG);
        window.setNavigationBarColor(Ui.BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
    }

    private View buildHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setClipToPadding(false);
        applySystemBarInsets(scroll);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(this, 22), Ui.dp(this, 20), Ui.dp(this, 22), Ui.dp(this, 28));
        scroll.addView(page, Ui.matchWrap());

        TextView eyebrow = Ui.title(this, "PUSH LOG", 14);
        eyebrow.setTextColor(Ui.GOLD);
        eyebrow.setLetterSpacing(0.16f);
        page.addView(eyebrow);

        TextView headline = Ui.title(this, "Show up. Add it up.", 28);
        LinearLayout.LayoutParams headlineParams = Ui.matchWrap();
        headlineParams.topMargin = Ui.dp(this, 4);
        page.addView(headline, headlineParams);

        TextView subhead = Ui.text(this, "Your push-ups, stored privately on this phone.", 14, Ui.MUTED);
        LinearLayout.LayoutParams subheadParams = Ui.matchWrap();
        subheadParams.topMargin = Ui.dp(this, 6);
        page.addView(subhead, subheadParams);

        ImageView hero = new ImageView(this);
        hero.setImageResource(R.drawable.hero_pushup);
        hero.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        hero.setAdjustViewBounds(true);
        hero.setContentDescription("Illustration of a man holding a push-up position");
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 224));
        heroParams.topMargin = Ui.dp(this, 8);
        heroParams.bottomMargin = Ui.dp(this, 8);
        page.addView(hero, heroParams);

        LinearLayout statsCard = new LinearLayout(this);
        statsCard.setOrientation(LinearLayout.HORIZONTAL);
        statsCard.setGravity(Gravity.CENTER_VERTICAL);
        statsCard.setPadding(Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18));
        statsCard.setBackground(Ui.rounded(Ui.CARD, 22, this));
        totalValue = addStat(statsCard, "—", "ALL TIME");
        addDivider(statsCard);
        bestValue = addStat(statsCard, "—", "BEST DAY");
        addDivider(statsCard);
        averageValue = addStat(statsCard, "—", "DAILY AVG");
        page.addView(statsCard, Ui.matchWrap());

        LinearLayout todayCard = new LinearLayout(this);
        todayCard.setOrientation(LinearLayout.HORIZONTAL);
        todayCard.setGravity(Gravity.CENTER_VERTICAL);
        todayCard.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        todayCard.setBackground(Ui.outlined(Ui.CARD_ALT, Ui.LINE, 20, this));
        LinearLayout.LayoutParams todayCardParams = Ui.matchWrap();
        todayCardParams.topMargin = Ui.dp(this, 12);
        page.addView(todayCard, todayCardParams);

        LinearLayout todayCopy = new LinearLayout(this);
        todayCopy.setOrientation(LinearLayout.VERTICAL);
        todayDate = Ui.title(this, "TODAY", 13);
        todayDate.setTextColor(Ui.GOLD);
        todayDate.setLetterSpacing(0.10f);
        todayCopy.addView(todayDate);
        TextView todayCaption = Ui.text(this, "push-ups completed", 14, Ui.MUTED);
        LinearLayout.LayoutParams captionParams = Ui.matchWrap();
        captionParams.topMargin = Ui.dp(this, 3);
        todayCopy.addView(todayCaption, captionParams);
        todayCard.addView(todayCopy, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        todayValue = Ui.title(this, "—", 34);
        todayValue.setGravity(Gravity.END);
        todayCard.addView(todayValue);

        TextView logButton = Ui.action(this, "+  Log today’s push-ups", true);
        logButton.setContentDescription("Log today's push-ups");
        logButton.setOnClickListener(view -> loadAndShowLogDialog());
        LinearLayout.LayoutParams logParams = Ui.matchWrap();
        logParams.topMargin = Ui.dp(this, 16);
        page.addView(logButton, logParams);

        TextView historyButton = Ui.action(this, "View monthly history", false);
        historyButton.setOnClickListener(view ->
                startActivity(new Intent(this, HistoryActivity.class)));
        LinearLayout.LayoutParams historyParams = Ui.matchWrap();
        historyParams.topMargin = Ui.dp(this, 10);
        page.addView(historyButton, historyParams);

        reminderButton = Ui.action(this, "Daily reminder  ·  Off", false);
        reminderButton.setOnClickListener(view -> openReminderSettings());
        LinearLayout.LayoutParams reminderParams = Ui.matchWrap();
        reminderParams.topMargin = Ui.dp(this, 10);
        page.addView(reminderButton, reminderParams);

        TextView reminderHint = Ui.text(
                this, "One alert at your chosen time—only when today is below 10.",
                12, Ui.MUTED);
        reminderHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams reminderHintParams = Ui.matchWrap();
        reminderHintParams.topMargin = Ui.dp(this, 7);
        page.addView(reminderHint, reminderHintParams);

        TextView importButton = Ui.action(this, "Import old app data (.puud)", false);
        importButton.setTextColor(Ui.GOLD);
        importButton.setOnClickListener(view -> openBackupPicker());
        LinearLayout.LayoutParams importParams = Ui.matchWrap();
        importParams.topMargin = Ui.dp(this, 22);
        page.addView(importButton, importParams);

        TextView privacy = Ui.text(this, "NO ADS  •  NO ACCOUNT  •  NO INTERNET ACCESS", 11, Ui.MUTED);
        privacy.setLetterSpacing(0.09f);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams privacyParams = Ui.matchWrap();
        privacyParams.topMargin = Ui.dp(this, 14);
        page.addView(privacy, privacyParams);

        return scroll;
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

    private TextView addStat(LinearLayout parent, String initial, String label) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER);
        TextView value = Ui.title(this, initial, 23);
        value.setGravity(Gravity.CENTER);
        block.addView(value);
        TextView caption = Ui.text(this, label, 10, Ui.MUTED);
        caption.setLetterSpacing(0.08f);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams = Ui.matchWrap();
        captionParams.topMargin = Ui.dp(this, 4);
        block.addView(caption, captionParams);
        parent.addView(block, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return value;
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(Ui.LINE);
        parent.addView(divider, new LinearLayout.LayoutParams(Ui.dp(this, 1), Ui.dp(this, 42)));
    }

    private void refreshDashboard() {
        io.execute(() -> {
            LocalDate today = LocalDate.now();
            PushupDatabase.Stats stats = database.getStats(today);
            int todayCount = database.getCount(today);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.getDefault());
                totalValue.setText(numbers.format(stats.total));
                bestValue.setText(numbers.format(stats.best));
                averageValue.setText(numbers.format(Math.round(stats.average)));
                todayValue.setText(numbers.format(todayCount));
                String date = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()));
                todayDate.setText("TODAY  •  " + date.toUpperCase(Locale.getDefault()));
            });
        });
    }

    private void loadAndShowLogDialog() {
        io.execute(() -> {
            int current = database.getCount(LocalDate.now());
            runOnUiThread(() -> showLogDialog(current));
        });
    }

    private void showLogDialog(int current) {
        int[] selected = {current};

        LinearLayout picker = new LinearLayout(this);
        picker.setOrientation(LinearLayout.VERTICAL);
        picker.setGravity(Gravity.CENTER);
        picker.setPadding(Ui.dp(this, 24), Ui.dp(this, 10), Ui.dp(this, 24), Ui.dp(this, 4));

        TextView caption = Ui.text(this, "TODAY’S TOTAL", 12, Ui.MUTED);
        caption.setLetterSpacing(0.10f);
        caption.setGravity(Gravity.CENTER);
        picker.addView(caption, Ui.matchWrap());

        TextView counter = Ui.title(this,
                NumberFormat.getIntegerInstance(Locale.getDefault()).format(current), 52);
        counter.setGravity(Gravity.CENTER);
        counter.setBackground(Ui.outlined(Ui.CARD_ALT, Ui.GOLD_DARK, 80, this));
        LinearLayout.LayoutParams counterParams = new LinearLayout.LayoutParams(
                Ui.dp(this, 150), Ui.dp(this, 150));
        counterParams.topMargin = Ui.dp(this, 12);
        counterParams.bottomMargin = Ui.dp(this, 18);
        picker.addView(counter, counterParams);

        LinearLayout increments = new LinearLayout(this);
        increments.setOrientation(LinearLayout.HORIZONTAL);
        increments.setGravity(Gravity.CENTER);

        TextView minusTen = Ui.action(this, "−10", false);
        minusTen.setTextSize(22);
        minusTen.setContentDescription("Subtract 10 push-ups");
        increments.addView(minusTen, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 62), 1f));

        TextView plusTen = Ui.action(this, "+10", true);
        plusTen.setTextSize(22);
        plusTen.setContentDescription("Add 10 push-ups");
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(
                0, Ui.dp(this, 62), 1f);
        plusParams.leftMargin = Ui.dp(this, 12);
        increments.addView(plusTen, plusParams);
        picker.addView(increments, Ui.matchWrap());

        View.OnClickListener refreshCounter = ignored -> counter.setText(
                NumberFormat.getIntegerInstance(Locale.getDefault()).format(selected[0]));
        minusTen.setOnClickListener(view -> {
            selected[0] = Math.max(0, selected[0] - 10);
            refreshCounter.onClick(view);
        });
        plusTen.setOnClickListener(view -> {
            selected[0] = Math.min(999_999, selected[0] + 10);
            refreshCounter.onClick(view);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Log push-ups")
                .setView(picker)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.GOLD);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> saveTodayTotal(selected[0], dialog));
        });
        dialog.show();
    }

    private void saveTodayTotal(int value, AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        io.execute(() -> {
            try {
                database.setCount(LocalDate.now(), value);
                if (value >= DailyReminderLogic.COMPLETION_THRESHOLD) {
                    ReminderNotifier.cancel(this);
                }
                runOnUiThread(() -> {
                    dialog.dismiss();
                    refreshDashboard();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this, failure.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openBackupPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/zip", "application/octet-stream", "application/x-sqlite3"});
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        new AlertDialog.Builder(this)
                .setTitle("Import old app history?")
                .setMessage("Dates covered by the backup will exactly match it, including clearing days whose total is zero. Any later dates already in Pushup Log will be kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Import", (dialog, which) -> importBackup(uri))
                .show();
    }

    private void importBackup(Uri uri) {
        Toast.makeText(this, "Importing history…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                PuudImporter.ImportResult result = PuudImporter.read(input, getCacheDir());
                database.replaceHistoryRange(
                        result.dailyCounts, result.firstDate, result.lastDate);
                if (database.getCount(LocalDate.now()) >=
                        DailyReminderLogic.COMPLETION_THRESHOLD) {
                    ReminderNotifier.cancel(this);
                }
                runOnUiThread(() -> {
                    refreshDashboard();
                    String summary = NumberFormat.getIntegerInstance().format(result.total) +
                            " push-ups across " + NumberFormat.getIntegerInstance().format(result.activeDays) +
                            " active days imported.";
                    if (result.clearedDays > 0) {
                        summary += "\n\n" + NumberFormat.getIntegerInstance().format(result.clearedDays) +
                                " zero-total days were cleared.";
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("History imported")
                            .setMessage(summary)
                            .setPositiveButton("Done", null)
                            .show();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Import failed")
                        .setMessage(failure.getMessage() == null ? "This backup could not be read." : failure.getMessage())
                        .setPositiveButton("OK", null)
                        .show());
            }
        });
    }

    private void openReminderSettings() {
        if (!ReminderPreferences.isEnabled(this)) {
            showReminderTimePicker();
            return;
        }

        String time = formatReminderTime(
                ReminderPreferences.hour(this), ReminderPreferences.minute(this));
        if (!ReminderNotifier.canPost(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Notifications are off")
                    .setMessage("Your daily reminder is set for " + time +
                            ", but Android is blocking Pushup Log notifications.")
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Turn off", (dialog, which) -> disableReminder())
                    .setPositiveButton("Open settings", (dialog, which) ->
                            openNotificationSettings())
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Daily reminder")
                .setMessage("Currently set for " + time +
                        ". Pushup Log sends one reminder only when today’s total is below 10.")
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Turn off", (dialog, which) -> disableReminder())
                .setPositiveButton("Change time", (dialog, which) ->
                        showReminderTimePicker())
                .show();
    }

    private void showReminderTimePicker() {
        int hour = ReminderPreferences.hour(this);
        int minute = ReminderPreferences.minute(this);
        new TimePickerDialog(
                this,
                (picker, selectedHour, selectedMinute) ->
                        requestNotificationPermissionOrEnable(selectedHour, selectedMinute),
                hour,
                minute,
                android.text.format.DateFormat.is24HourFormat(this))
                .show();
    }

    private void requestNotificationPermissionOrEnable(int hour, int minute) {
        ReminderNotifier.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            // Preserve the selected schedule even if Android sends the user to
            // system settings or the activity is recreated during permission flow.
            ReminderPreferences.enable(this, hour, minute);
            ReminderScheduler.scheduleNext(this);
            updateReminderButton();
            pendingReminderHour = hour;
            pendingReminderMinute = minute;
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
            return;
        }
        enableReminder(hour, minute);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) {
            return;
        }
        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enablePendingReminder();
        } else {
            showNotificationPermissionRequired();
        }
    }

    private void enablePendingReminder() {
        if (pendingReminderHour < 0) {
            return;
        }
        int hour = pendingReminderHour;
        int minute = pendingReminderMinute;
        pendingReminderHour = -1;
        pendingReminderMinute = -1;
        enableReminder(hour, minute);
    }

    private void enableReminder(int hour, int minute) {
        ReminderPreferences.enable(this, hour, minute);
        ReminderScheduler.scheduleNext(this);
        updateReminderButton();
        Toast.makeText(
                this,
                "Reminder set for " + formatReminderTime(hour, minute),
                Toast.LENGTH_SHORT).show();
        if (!ReminderNotifier.canPost(this)) {
            showNotificationPermissionRequired();
        }
    }

    private void disableReminder() {
        ReminderPreferences.disable(this);
        ReminderScheduler.cancel(this);
        ReminderNotifier.cancel(this);
        updateReminderButton();
        Toast.makeText(this, "Daily reminder turned off", Toast.LENGTH_SHORT).show();
    }

    private void updateReminderButton() {
        if (reminderButton == null) {
            return;
        }
        if (!ReminderPreferences.isEnabled(this)) {
            reminderButton.setText("Daily reminder  ·  Off");
            reminderButton.setContentDescription("Set daily push-up reminder");
            return;
        }
        if (!ReminderNotifier.canPost(this)) {
            reminderButton.setText("Daily reminder  ·  Permission needed");
            reminderButton.setContentDescription(
                    "Daily push-up reminder needs notification permission");
            return;
        }
        String time = formatReminderTime(
                ReminderPreferences.hour(this), ReminderPreferences.minute(this));
        reminderButton.setText("Daily reminder  ·  " + time);
        reminderButton.setContentDescription("Daily push-up reminder set for " + time);
    }

    private String formatReminderTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        return android.text.format.DateFormat.getTimeFormat(this)
                .format(calendar.getTime());
    }

    private void showNotificationPermissionRequired() {
        new AlertDialog.Builder(this)
                .setTitle("Allow notifications")
                .setMessage("Pushup Log needs notification permission to deliver your daily reminder. No notifications are used for ads or anything else.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Open settings", (dialog, which) ->
                        openNotificationSettings())
                .show();
    }

    private void openNotificationSettings() {
        Intent settings = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(settings);
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
