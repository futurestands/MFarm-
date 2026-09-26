package dev.mfarm.com.mfarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public final class AlarmScheduler {
    public static final String EXTRA_ANIMAL_NAME = "animal_name";
    public static final String EXTRA_VACCINE_NAME = "vaccine_name";
    public static final String EXTRA_VACCINATION_ID = "vaccination_id";

    private static final String TAG = "AlarmScheduler";
    private static final String PREFS_NAME = "vaccination_alarm_prefs";
    private static final String PREF_NOTIFIED_PREFIX = "notified_date_";

    private AlarmScheduler() {}

    public static boolean hasBeenNotifiedToday(Context context, int vaccinationId, String todayDateStr) {
        if (context == null || todayDateStr == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String notifiedDate = prefs.getString(PREF_NOTIFIED_PREFIX + vaccinationId, null);
        return todayDateStr.equals(notifiedDate);
    }

    public static void markNotifiedToday(Context context, int vaccinationId, String todayDateStr) {
        if (context == null || todayDateStr == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_NOTIFIED_PREFIX + vaccinationId, todayDateStr).apply();
    }

    public static void clearNotified(Context context, int vaccinationId) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(PREF_NOTIFIED_PREFIX + vaccinationId).apply();
    }

    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = alarmManager(context);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    public static Intent exactAlarmSettingsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    public static boolean schedule(Context context, int vaccinationId, String animalName,
                                   String vaccineName, String scheduledDate) {
        long nowMillis = System.currentTimeMillis();
        TimeZone timeZone = TimeZone.getDefault();
        long triggerAt = VaccinationAlarmTime.triggerMillis(
                scheduledDate, nowMillis, timeZone);
        if (triggerAt < 0) {
            Log.i(TAG, "Skipping past vaccination alarm id=" + vaccinationId);
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(VaccinationAlarmTime.DATE_PATTERN, Locale.US);
        sdf.setTimeZone(timeZone);
        String todayDateStr = sdf.format(new Date(nowMillis));

        // Avoid firing repeated notifications on same-day app launches if already notified
        if (scheduledDate.equals(todayDateStr) && triggerAt > nowMillis && triggerAt <= nowMillis + 10_000L) {
            if (hasBeenNotifiedToday(context, vaccinationId, todayDateStr)) {
                Log.i(TAG, "Already notified today for vaccination id=" + vaccinationId);
                return false;
            }
        }

        return scheduleAt(context, vaccinationId, animalName, vaccineName, triggerAt);
    }

    public static boolean scheduleAt(Context context, int vaccinationId, String animalName,
                                     String vaccineName, long triggerAtMillis) {
        AlarmManager alarmManager = alarmManager(context);
        if (alarmManager == null) {
            return false;
        }
        PendingIntent pendingIntent = reminderPendingIntent(
                context, vaccinationId, animalName, vaccineName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canScheduleExact(context)) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } catch (SecurityException e) {
                    Log.w(TAG, "SecurityException on setExactAndAllowWhileIdle, falling back to inexact", e);
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
        return true;
    }

    public static void cancel(Context context, int vaccinationId) {
        AlarmManager alarmManager = alarmManager(context);
        if (alarmManager != null) {
            PendingIntent pendingIntent = reminderPendingIntent(context, vaccinationId, "", "");
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        clearNotified(context, vaccinationId);
    }

    public static void updateOverdueStatus(SQLiteDatabase db) {
        if (db == null || !db.isOpen()) return;
        SimpleDateFormat sdf = new SimpleDateFormat(VaccinationAlarmTime.DATE_PATTERN, Locale.US);
        sdf.setLenient(false);
        String todayStr = sdf.format(new Date());
        try {
            Date today = sdf.parse(todayStr);
            Cursor cursor = db.rawQuery(
                    "SELECT id, scheduled_date FROM vaccinations WHERE status = 'Pending'", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(0);
                    String schedStr = cursor.getString(1);
                    if (schedStr != null && !schedStr.trim().isEmpty()) {
                        try {
                            Date schedDate = sdf.parse(schedStr.trim());
                            if (schedDate != null && schedDate.before(today)) {
                                ContentValues cv = new ContentValues();
                                cv.put("status", "Overdue");
                                db.update("vaccinations", cv, "id=?", new String[]{String.valueOf(id)});
                            }
                        } catch (ParseException ignored) {}
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating overdue vaccinations", e);
        }
    }

    public static int reschedulePending(Context context) {
        SQLiteDatabase db = openDatabase(context);
        if (db == null) {
            return 0;
        }
        updateOverdueStatus(db);
        int scheduled = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT v.id, v.vaccine_name, v.scheduled_date, a.name "
                            + "FROM vaccinations v "
                            + "JOIN animas a ON v.animal_id = a.id "
                            + "WHERE v.status = ?",
                    new String[]{"Pending"});
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String vaccineName = cursor.getString(1);
                String scheduledDate = cursor.getString(2);
                String animalName = cursor.getString(3);
                try {
                    if (schedule(context, id, animalName, vaccineName, scheduledDate)) {
                        scheduled++;
                    }
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Bad vaccination date for id=" + id, e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reschedule vaccination alarms", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return scheduled;
    }

    static SQLiteDatabase openDatabase(Context context) {
        if (MainActivity.database != null && MainActivity.database.isOpen()) {
            return MainActivity.database;
        }
        try {
            DatabaseHelper helper = DatabaseHelper.getHelper(context.getApplicationContext());
            SQLiteDatabase db = helper.openDataBase();
            MainActivity.database = db;
            return db;
        } catch (Exception e) {
            Log.e(TAG, "Could not open farm database for alarms", e);
            return null;
        }
    }

    private static PendingIntent reminderPendingIntent(Context context, int vaccinationId,
                                                       String animalName, String vaccineName) {
        Intent intent = new Intent(context, VaccinationReminderReceiver.class);
        intent.setAction("dev.mfarm.com.mfarm.VACCINATION_REMINDER");
        intent.putExtra(EXTRA_VACCINATION_ID, vaccinationId);
        intent.putExtra(EXTRA_ANIMAL_NAME, animalName);
        intent.putExtra(EXTRA_VACCINE_NAME, vaccineName);
        return PendingIntent.getBroadcast(
                context.getApplicationContext(),
                vaccinationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static AlarmManager alarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}
