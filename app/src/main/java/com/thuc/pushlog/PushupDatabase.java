package com.thuc.pushlog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

final class PushupDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "pushlog.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "daily_pushups";

    PushupDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "date TEXT PRIMARY KEY NOT NULL," +
                "count INTEGER NOT NULL CHECK(count >= 0)," +
                "updated_at INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 has no migrations yet. Future versions must preserve this table.
    }

    synchronized boolean isEmpty() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM " + TABLE + " LIMIT 1", null)) {
            return !cursor.moveToFirst();
        }
    }

    synchronized int getCount(LocalDate date) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE,
                new String[]{"count"},
                "date = ?",
                new String[]{date.toString()},
                null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    synchronized void addCount(LocalDate date, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int current = getCount(date);
        long next = (long) current + amount;
        if (next > 999_999) {
            throw new IllegalArgumentException("Daily count is too large");
        }
        setCount(date, (int) next);
    }

    synchronized void setCount(LocalDate date, int count) {
        if (count < 0 || count > 999_999) {
            throw new IllegalArgumentException("Count must be between 0 and 999,999");
        }
        SQLiteDatabase db = getWritableDatabase();
        if (count == 0) {
            db.delete(TABLE, "date = ?", new String[]{date.toString()});
            return;
        }
        ContentValues values = new ContentValues();
        values.put("date", date.toString());
        values.put("count", count);
        values.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    synchronized void replaceHistoryRange(Map<String, Integer> dailyCounts,
                                          LocalDate firstDate, LocalDate lastDate) {
        if (firstDate == null || lastDate == null || firstDate.isAfter(lastDate)) {
            throw new IllegalArgumentException("The backup date range is invalid");
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // The imported backup is authoritative for its date range. Clearing
            // the range first removes stale local values for zero or absent days.
            db.delete(TABLE, "date >= ? AND date <= ?",
                    new String[]{firstDate.toString(), lastDate.toString()});

            ContentValues values = new ContentValues();
            for (Map.Entry<String, Integer> entry : dailyCounts.entrySet()) {
                int count = entry.getValue();
                if (count <= 0) {
                    continue;
                }
                values.clear();
                values.put("date", entry.getKey());
                values.put("count", count);
                values.put("updated_at", System.currentTimeMillis());
                db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    synchronized Stats getStats(LocalDate today) {
        long total = 0;
        int best = 0;
        String first = null;
        String last = null;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(count), 0), COALESCE(MAX(count), 0), " +
                        "MIN(date), MAX(date) FROM " + TABLE, null)) {
            if (cursor.moveToFirst()) {
                total = cursor.getLong(0);
                best = cursor.getInt(1);
                first = cursor.isNull(2) ? null : cursor.getString(2);
                last = cursor.isNull(3) ? null : cursor.getString(3);
            }
        }

        double average = 0;
        if (first != null) {
            LocalDate firstDate = LocalDate.parse(first);
            LocalDate endDate = LocalDate.parse(last);
            if (today.isAfter(endDate)) {
                endDate = today;
            }
            long days = Math.max(1, ChronoUnit.DAYS.between(firstDate, endDate) + 1);
            average = total / (double) days;
        }
        return new Stats(total, best, average, first, last);
    }

    synchronized MonthData getMonth(YearMonth month) {
        String start = month.atDay(1).toString();
        String end = month.atEndOfMonth().toString();
        Map<LocalDate, Integer> days = new LinkedHashMap<>();
        int total = 0;
        int best = 0;
        try (Cursor cursor = getReadableDatabase().query(
                TABLE,
                new String[]{"date", "count"},
                "date >= ? AND date <= ?",
                new String[]{start, end},
                null, null, "date ASC")) {
            while (cursor.moveToNext()) {
                LocalDate date = LocalDate.parse(cursor.getString(0));
                int count = cursor.getInt(1);
                days.put(date, count);
                total += count;
                best = Math.max(best, count);
            }
        }
        return new MonthData(days, total, best, days.size());
    }

    static final class Stats {
        final long total;
        final int best;
        final double average;
        final String firstDate;
        final String lastDate;

        Stats(long total, int best, double average, String firstDate, String lastDate) {
            this.total = total;
            this.best = best;
            this.average = average;
            this.firstDate = firstDate;
            this.lastDate = lastDate;
        }
    }

    static final class MonthData {
        final Map<LocalDate, Integer> days;
        final int total;
        final int best;
        final int activeDays;

        MonthData(Map<LocalDate, Integer> days, int total, int best, int activeDays) {
            this.days = days;
            this.total = total;
            this.best = best;
            this.activeDays = activeDays;
        }
    }
}
