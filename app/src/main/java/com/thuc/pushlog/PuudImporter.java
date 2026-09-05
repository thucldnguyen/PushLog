package com.thuc.pushlog;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class PuudImporter {
    private static final long MAX_DATABASE_BYTES = 25L * 1024L * 1024L;

    private PuudImporter() {}

    static ImportResult read(InputStream source, File cacheDirectory) throws IOException {
        if (source == null) {
            throw new IOException("The selected file could not be opened");
        }
        File extracted = File.createTempFile("pushlog-import-", ".db", cacheDirectory);
        boolean foundDatabase = false;
        try {
            try (ZipInputStream zip = new ZipInputStream(source)) {
                ZipEntry entry;
                byte[] buffer = new byte[32 * 1024];
                while ((entry = zip.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith("PushUps_Mos.db")) {
                        long written = 0;
                        try (FileOutputStream output = new FileOutputStream(extracted)) {
                            int read;
                            while ((read = zip.read(buffer)) != -1) {
                                written += read;
                                if (written > MAX_DATABASE_BYTES) {
                                    throw new IOException("The database inside this backup is unexpectedly large");
                                }
                                output.write(buffer, 0, read);
                            }
                        }
                        foundDatabase = true;
                        break;
                    }
                    zip.closeEntry();
                }
            }

            if (!foundDatabase || extracted.length() == 0) {
                throw new IOException("This is not a compatible Push Ups backup (.puud)");
            }
            return readDatabase(extracted);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            extracted.delete();
        }
    }

    private static ImportResult readDatabase(File databaseFile) throws IOException {
        SQLiteDatabase database = null;
        try {
            database = SQLiteDatabase.openDatabase(
                    databaseFile.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);

            try (Cursor table = database.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name='PushUpsRecord'", null)) {
                if (!table.moveToFirst()) {
                    throw new IOException("Push-up history was not found in this backup");
                }
            }

            int rawRows;
            try (Cursor count = database.rawQuery("SELECT COUNT(*) FROM PushUpsRecord", null)) {
                if (!count.moveToFirst()) {
                    throw new IOException("Could not read the backup history");
                }
                rawRows = count.getInt(0);
            }

            Map<String, Integer> daily = new LinkedHashMap<>();
            long total = 0;
            LocalDate first = null;
            LocalDate last = null;
            int activeDays = 0;
            int clearedDays = 0;
            try (Cursor cursor = database.rawQuery(
                    "SELECT year, month, day, SUM(num) AS daily_total " +
                            "FROM PushUpsRecord GROUP BY year, month, day " +
                            "ORDER BY year, month, day", null)) {
                while (cursor.moveToNext()) {
                    int year = cursor.getInt(0);
                    int month = cursor.getInt(1);
                    int day = cursor.getInt(2);
                    long value = cursor.getLong(3);
                    LocalDate date;
                    try {
                        date = LocalDate.of(year, month, day);
                    } catch (DateTimeException invalidDate) {
                        throw new IOException("The backup contains an invalid workout date", invalidDate);
                    }
                    if (value < 0 || value > 999_999) {
                        throw new IOException("The backup contains an invalid daily push-up count");
                    }
                    daily.put(date.toString(), (int) value);
                    total += value;
                    if (first == null) {
                        first = date;
                    }
                    last = date;
                    if (value == 0) {
                        clearedDays++;
                    } else {
                        activeDays++;
                    }
                }
            }

            if (daily.isEmpty()) {
                throw new IOException("No push-up records were found in this backup");
            }
            return new ImportResult(
                    daily, rawRows, total, first, last, activeDays, clearedDays);
        } catch (RuntimeException failure) {
            throw new IOException("The backup database could not be read", failure);
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    static final class ImportResult {
        final Map<String, Integer> dailyCounts;
        final int rawRows;
        final long total;
        final LocalDate firstDate;
        final LocalDate lastDate;
        final int activeDays;
        final int clearedDays;

        ImportResult(Map<String, Integer> dailyCounts, int rawRows, long total,
                     LocalDate firstDate, LocalDate lastDate,
                     int activeDays, int clearedDays) {
            this.dailyCounts = dailyCounts;
            this.rawRows = rawRows;
            this.total = total;
            this.firstDate = firstDate;
            this.lastDate = lastDate;
            this.activeDays = activeDays;
            this.clearedDays = clearedDays;
        }
    }
}
