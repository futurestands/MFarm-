package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class AddBreedingActivity extends AppCompatActivity {

    private Spinner spinnerAnimals, spinnerSpecies;
    private TextInputEditText etBullId;
    private Button btnMatingDate, btnSave;
    private TextView tvExpectedBirth;
    private Calendar matingCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> animalNames = new ArrayList<>();
    private List<Integer> animalIds = new ArrayList<>();
    private String expectedBirthDateStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_breeding);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Breeding Record");
        }

        spinnerAnimals = findViewById(R.id.spinnerAnimals);
        etBullId = findViewById(R.id.etBullId);
        btnMatingDate = findViewById(R.id.btnMatingDate);
        btnSave = findViewById(R.id.btnSave);
        tvExpectedBirth = findViewById(R.id.tvExpectedBirth);

        loadAnimals();

        btnMatingDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRecord();
            }
        });
    }

    private void loadAnimals() {
        animalIds.clear();
        animalNames.clear();
        Cursor cursor = MainActivity.database.rawQuery("SELECT id, name FROM animas WHERE gender = 'Female' OR gender = '1' OR LOWER(gender) = 'female'", null);
        if (cursor.moveToFirst()) {
            do {
                animalIds.add(cursor.getInt(0));
                animalNames.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (animalNames.isEmpty()) {
            animalNames.add("No Female Animals Found");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnimals.setAdapter(adapter);
    }

    private void showDatePicker() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                matingCalendar.set(Calendar.YEAR, year);
                matingCalendar.set(Calendar.MONTH, month);
                matingCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String matingDate = dateFormatter.format(matingCalendar.getTime());
                btnMatingDate.setText(matingDate);

                // Default cattle gestation ~283 days
                int gestationDays = 283;
                Calendar birthCalendar = (Calendar) matingCalendar.clone();
                birthCalendar.add(Calendar.DAY_OF_YEAR, gestationDays);
                expectedBirthDateStr = dateFormatter.format(birthCalendar.getTime());
                tvExpectedBirth.setText("Expected Birth Date (~" + gestationDays + " days): " + expectedBirthDateStr);
            }
        }, matingCalendar.get(Calendar.YEAR), matingCalendar.get(Calendar.MONTH), matingCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveRecord() {
        int selectedPos = spinnerAnimals.getSelectedItemPosition();
        if (animalIds.isEmpty() || selectedPos < 0 || selectedPos >= animalIds.size()) {
            Toast.makeText(this, "Please select an animal", Toast.LENGTH_SHORT).show();
            return;
        }

        int animalId = animalIds.get(selectedPos);
        String bullId = etBullId.getText().toString().trim();
        String matingDate = btnMatingDate.getText().toString();

        if (expectedBirthDateStr.isEmpty()) {
            Toast.makeText(this, "Please select mating date", Toast.LENGTH_SHORT).show();
            return;
        }

        MainActivity.database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("animal_id", animalId);
            values.put("mating_date", matingDate);
            values.put("bull_id", bullId);
            values.put("expected_birth_date", expectedBirthDateStr);
            values.put("status", "Pregnant");
            values.put("pregnancy_confirmed", 1);

            long rowId = MainActivity.database.insert("breeding_records", null, values);

            // Update animal repro_status to Pregnant
            ContentValues reproValues = new ContentValues();
            reproValues.put("repro_status", "Pregnant");
            MainActivity.database.update("animas", reproValues, "id=?", new String[]{String.valueOf(animalId)});

            MainActivity.database.setTransactionSuccessful();
            DatabaseHelper.logAudit(MainActivity.database, "CREATE_BREEDING", "BREEDING", rowId, "Recorded breeding for Animal ID: " + animalId);

            Toast.makeText(this, "Breeding Record Saved Successfully", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            MainActivity.database.endTransaction();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
