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

public class RegisterCalvingActivity extends AppCompatActivity {

    private Spinner spinnerDam, spinnerSex, spinnerSurvival;
    private TextInputEditText etSireId, etCalfName, etBirthWeight, etNotes;
    private Button btnCalvingDate, btnSaveCalving;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> damNames = new ArrayList<>();
    private List<Integer> damIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_calving);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Register Birth / Calving");
        }

        spinnerDam = findViewById(R.id.spinnerDam);
        spinnerSex = findViewById(R.id.spinnerSex);
        spinnerSurvival = findViewById(R.id.spinnerSurvival);
        etSireId = findViewById(R.id.etSireId);
        etCalfName = findViewById(R.id.etCalfName);
        etBirthWeight = findViewById(R.id.etBirthWeight);
        etNotes = findViewById(R.id.etNotes);
        btnCalvingDate = findViewById(R.id.btnCalvingDate);
        btnSaveCalving = findViewById(R.id.btnSaveCalving);

        setupSpinners();
        loadDams();
        updateDateButton();

        btnCalvingDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(RegisterCalvingActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButton();
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btnSaveCalving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCalving();
            }
        });
    }

    private void updateDateButton() {
        btnCalvingDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private void setupSpinners() {
        String[] sexes = {"Female", "Male"};
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sexes);
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSex.setAdapter(sexAdapter);

        String[] statuses = {"Alive", "Stillborn"};
        ArrayAdapter<String> survivalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        survivalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSurvival.setAdapter(survivalAdapter);
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return DatabaseHelper.getDatabase(this);
    }

    private void loadDams() {
        damIds.clear();
        damNames.clear();
        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT id, name FROM animas WHERE gender = 'Female' OR gender = '1' OR LOWER(gender) = 'female'", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                damIds.add(cursor.getInt(0));
                damNames.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        if (damNames.isEmpty()) {
            damNames.add("No Female Animals Registered");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, damNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDam.setAdapter(adapter);
    }

    private void saveCalving() {
        int selectedDamPos = spinnerDam.getSelectedItemPosition();
        if (damIds.isEmpty() || selectedDamPos < 0 || selectedDamPos >= damIds.size()) {
            Toast.makeText(this, "Please select a dam", Toast.LENGTH_SHORT).show();
            return;
        }

        int damId = damIds.get(selectedDamPos);
        String calfName = etCalfName.getText().toString().trim();
        String sireId = etSireId.getText().toString().trim();
        String sex = spinnerSex.getSelectedItem().toString();
        String survival = spinnerSurvival.getSelectedItem().toString();
        String weightStr = etBirthWeight.getText().toString().trim();
        double weight = weightStr.isEmpty() ? 0 : Double.parseDouble(weightStr);
        String dateStr = btnCalvingDate.getText().toString();
        String notes = etNotes.getText().toString().trim();

        if (calfName.isEmpty()) {
            Toast.makeText(this, "Please enter offspring name or tag", Toast.LENGTH_SHORT).show();
            return;
        }

        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;

        db.beginTransaction();
        try {
            long offspringId = -1;
            if ("Alive".equalsIgnoreCase(survival)) {
                ContentValues calfValues = new ContentValues();
                calfValues.put("name", calfName);
                calfValues.put("breed_id", 1);
                calfValues.put("gender", sex);
                calfValues.put("dob", dateStr);
                calfValues.put("dam_id", String.valueOf(damId));
                calfValues.put("sire_id", sireId.isEmpty() ? "1" : sireId);
                calfValues.put("lifecycle_status", "Active");
                calfValues.put("repro_status", "Open");
                calfValues.put("lactation_status", "Dry");
                calfValues.put("health_status", "Healthy");

                offspringId = db.insert("animas", null, calfValues);
            }

            ContentValues calvingValues = new ContentValues();
            calvingValues.put("dam_id", damId);
            calvingValues.put("sire_id", sireId);
            calvingValues.put("birth_date", dateStr);
            calvingValues.put("offspring_id", offspringId);
            calvingValues.put("sex", sex);
            calvingValues.put("birth_weight", weight);
            calvingValues.put("survival_status", survival);
            calvingValues.put("notes", notes);

            long recordId = db.insert("calving_records", null, calvingValues);

            // Update Dam's status to Lactating & Calved, and mark pending breeding record as Calved
            ContentValues damStatus = new ContentValues();
            damStatus.put("lactation_status", "Lactating");
            damStatus.put("repro_status", "Calved");
            db.update("animas", damStatus, "id=?", new String[]{String.valueOf(damId)});

            ContentValues breedingStatus = new ContentValues();
            breedingStatus.put("status", "Calved");
            db.update("breeding_records", breedingStatus, "animal_id=? AND status='Pregnant'", new String[]{String.valueOf(damId)});

            db.setTransactionSuccessful();
            DatabaseHelper.logAudit(db, "RECORD_CALVING", "BREEDING", recordId, "Recorded calving for Dam ID: " + damId + " | Offspring: " + calfName);

            Toast.makeText(this, "Calving Registered Successfully", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving calving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
