package dev.mfarm.com.mfarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VaccinationReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "VaccinationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int vaccinationId = intent.getIntExtra(AlarmScheduler.EXTRA_VACCINATION_ID, 0);
        String animalName = intent.getStringExtra(AlarmScheduler.EXTRA_ANIMAL_NAME);
        String vaccineName = intent.getStringExtra(AlarmScheduler.EXTRA_VACCINE_NAME);

        if (vaccinationId > 0) {
            SQLiteDatabase db = AlarmScheduler.openDatabase(context);
            if (db != null) {
                Cursor c = null;
                try {
                    c = db.rawQuery(
                            "SELECT v.status, a.name, v.vaccine_name " +
                            "FROM vaccinations v " +
                            "JOIN animas a ON v.animal_id = a.id " +
                            "WHERE v.id = ?",
                            new String[]{String.valueOf(vaccinationId)});
                    if (c != null && c.moveToFirst()) {
                        String status = c.getString(0);
                        if ("Completed".equalsIgnoreCase(status)) {
                            Log.i(TAG, "Vaccination id " + vaccinationId + " is completed, skipping notification");
                            return;
                        }
                        String dbAnimalName = c.getString(1);
                        String dbVaccineName = c.getString(2);
                        if (dbAnimalName != null && !dbAnimalName.trim().isEmpty()) {
                            animalName = dbAnimalName;
                        }
                        if (dbVaccineName != null && !dbVaccineName.trim().isEmpty()) {
                            vaccineName = dbVaccineName;
                        }
                    } else {
                        // Vaccination or animal record was deleted
                        Log.i(TAG, "Vaccination or animal deleted for id " + vaccinationId + ", skipping notification");
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error verifying vaccination in receiver", e);
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }

        if (animalName == null || animalName.trim().isEmpty()) {
            animalName = "an animal";
        }
        if (vaccineName == null || vaccineName.trim().isEmpty()) {
            vaccineName = "the scheduled vaccine";
        }

        int notificationId = vaccinationId > 0 ? vaccinationId : (int) System.currentTimeMillis();
        NotificationHelper notificationHelper = new NotificationHelper(context);
        NotificationCompat.Builder nb = notificationHelper.getVaccinationNotification(
                "Vaccination Reminder",
                "It's time to vaccinate " + animalName + " with " + vaccineName,
                notificationId
        );
        notificationHelper.getManager().notify(notificationId, nb.build());

        if (vaccinationId > 0) {
            String todayDateStr = new SimpleDateFormat(VaccinationAlarmTime.DATE_PATTERN, Locale.US).format(new Date());
            AlarmScheduler.markNotifiedToday(context, vaccinationId, todayDateStr);
        }
    }
}
