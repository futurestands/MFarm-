package dev.mfarm.com.mfarm;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleVaccinationActivity extends AppCompatActivity {

    private Spinner spinnerAnimals;
    private TextInputEditText etVaccineName, etRemarks;
    private Button btnDate, btnSave;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> animalNames = new ArrayList<>();
    private List<Integer> animalIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_vaccination);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Schedule Vaccination");
        }

        spinnerAnimals = findViewById(R.id.spinnerAnimals);
        etVaccineName = findViewById(R.id.etVaccineName);
        etRemarks = findViewById(R.id.etRemarks);
        btnDate = findViewById(R.id.btnDate);
        btnSave = findViewById(R.id.btnSave);

        snapCalendarToReminderHour();
        loadAnimals();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkNotificationPermission()) {
                    saveVaccination();
                }
            }
        });

        updateDateButton();
    }

    private void loadAnimals() {
        Cursor cursor = MainActivity.database.rawQuery("SELECT id, name FROM animas", null);
        if (cursor.moveToFirst()) {
            do {
                animalIds.add(cursor.getInt(0));
                animalNames.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnimals.setAdapter(adapter);
    }

    private void showDatePicker() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                snapCalendarToReminderHour();
                updateDateButton();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButton() {
        btnDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private void saveVaccination() {
        int selectedPos = spinnerAnimals.getSelectedItemPosition();
        if (animalIds.isEmpty() || selectedPos < 0 || selectedPos >= animalIds.size()) {
            Toast.makeText(this, "Please select an animal", Toast.LENGTH_SHORT).show();
            return;
        }

        int animalId = animalIds.get(selectedPos);
        String animalName = animalNames.get(selectedPos);
        String vaccineName = etVaccineName.getText().toString().trim();
        String remarks = etRemarks.getText().toString().trim();
        String date = btnDate.getText().toString();

        if (vaccineName.isEmpty()) {
            Toast.makeText(this, "Please enter vaccine name", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isPast = false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            sdf.setLenient(false);
            Date schedDate = sdf.parse(date);
            Date today = sdf.parse(sdf.format(new Date()));
            if (schedDate != null && schedDate.before(today)) {
                isPast = true;
            }
        } catch (Exception ignored) {}

        ContentValues values = new ContentValues();
        values.put("animal_id", animalId);
        values.put("vaccine_name", vaccineName);
        values.put("scheduled_date", date);
        values.put("status", isPast ? "Overdue" : "Pending");
        values.put("remarks", remarks);

        try {
            long id = MainActivity.database.insert("vaccinations", null, values);
            if (id != -1) {
                boolean alarmSet = AlarmScheduler.schedule(
                        this, (int) id, animalName, vaccineName, date);
                if (alarmSet) {
                    if (AlarmScheduler.canScheduleExact(this)) {
                        Toast.makeText(this, "Scheduled for 8:00 AM on " + date, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Scheduled for " + date + " (inexact timing; enable exact alarms for 8:00 AM precision)", Toast.LENGTH_LONG).show();
                        if (!promptForExactAlarmsIfNeeded()) {
                            finish();
                        }
                    }
                } else {
                    Toast.makeText(this,
                            "Saved as overdue (" + date + "). No reminder was set.",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void snapCalendarToReminderHour() {
        calendar.set(Calendar.HOUR_OF_DAY, VaccinationAlarmTime.REMINDER_HOUR);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean promptForExactAlarmsIfNeeded() {
        if (AlarmScheduler.canScheduleExact(this)) {
            return false;
        }
        new AlertDialog.Builder(this)
                .setTitle("Exact reminders")
                .setMessage("Android may delay vaccination alerts unless MFarm is allowed to set exact alarms. Enable this so shots fire at 8:00 AM even after a reboot.")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(AlarmScheduler.exactAlarmSettingsIntent(ScheduleVaccinationActivity.this));
                        } catch (Exception ignored) {
                            Toast.makeText(ScheduleVaccinationActivity.this,
                                    "Open Settings → Alarms & reminders and allow MFarm.",
                                    Toast.LENGTH_LONG).show();
                        }
                        finish();
                    }
                })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
        return true;
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveVaccination();
            } else {
                Toast.makeText(this, "Notification permission denied. Reminder will not be shown.", Toast.LENGTH_SHORT).show();
                saveVaccination();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
