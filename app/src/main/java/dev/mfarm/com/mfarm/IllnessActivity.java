package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class IllnessActivity extends AppCompatActivity {
    Spinner spinneranimal, spinnerdisease;
    View layoutCustomDisease;
    TextInputEditText etCustomDisease;
    private ArrayAdapter<String> adapterForSpinner;
    String animalName = "";

    int animal_id = 0;
    EditText edtsymptoms;
    Button btnSave, btndate;
    String disease = "";
    String dateOccured = "";
    String preSelectedType = "";
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_illness);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Illness Occurrence");
        }

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        spinneranimal = findViewById(R.id.spinner);
        spinnerdisease = findViewById(R.id.spinnerdisease);
        layoutCustomDisease = findViewById(R.id.layoutCustomDisease);
        etCustomDisease = findViewById(R.id.etCustomDisease);
        edtsymptoms = findViewById(R.id.edtsymptoms);
        btnSave = findViewById(R.id.btnSave);
        btndate = findViewById(R.id.btndate);

        if (getIntent().hasExtra("type")) {
            preSelectedType = getIntent().getStringExtra("type");
        }

        updateDateButton();

        btndate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dpd = new DatePickerDialog(IllnessActivity.this, R.style.GreenDatePickerTheme, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButton();
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
                dpd.show();
            }
        });
        loadAnimalData();
        loadDiseasesData();

        spinneranimal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null && view instanceof TextView) {
                    animalName = ((TextView) view).getText().toString();
                } else {
                    animalName = parent.getItemAtPosition(position).toString();
                }
                android.database.sqlite.SQLiteDatabase db = getDb();
                if (db != null) {
                    Cursor cursor = db.rawQuery("SELECT id,name FROM animas where name = ?", new String[]{animalName});
                    if (cursor != null && cursor.moveToFirst()) {
                        animal_id = cursor.getInt(0);
                    }
                    if (cursor != null) cursor.close();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerdisease.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                disease = parent.getItemAtPosition(position).toString();
                if (layoutCustomDisease != null) {
                    if (disease.contains("Other")) {
                        layoutCustomDisease.setVisibility(View.VISIBLE);
                    } else {
                        layoutCustomDisease.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveIllnessRecord();
            }
        });
    }

    private void updateDateButton() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        dateOccured = sdf.format(calendar.getTime());
        btndate.setText("📅 Date: " + dateOccured);
    }

    private void saveIllnessRecord() {
        if (disease.contains("Other") && etCustomDisease != null && !etCustomDisease.getText().toString().trim().isEmpty()) {
            disease = etCustomDisease.getText().toString().trim();
        } else if (disease.contains("Other")) {
            Toast.makeText(getApplicationContext(), "Please specify custom disease name", Toast.LENGTH_LONG).show();
            return;
        }

        if (edtsymptoms.getText().toString().trim().equalsIgnoreCase("")) {
            Toast.makeText(getApplicationContext(), "Please Enter symptoms observed", Toast.LENGTH_LONG).show();
            return;
        }

        if (animal_id == 0) {
            Toast.makeText(getApplicationContext(), "Please Select Animal", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues collect = new ContentValues();
        collect.put("animal_id", animal_id);
        collect.put("animal_name", animalName);
        collect.put("sings_noted", edtsymptoms.getText().toString().trim());
        collect.put("illness_occured", disease);
        collect.put("treatment", "");
        collect.put("date_occured", dateOccured);
        collect.put("diagnosis", "");
        collect.put("medicine", "");
        collect.put("treatment_date", "");
        collect.put("others", "");

        try {
            android.database.sqlite.SQLiteDatabase db = getDb();
            if (db != null) {
                db.insert("illness", null, collect);
            }
            Toast.makeText(getApplicationContext(), "Illness Record Saved Successfully", Toast.LENGTH_LONG).show();

            Intent pp = new Intent(getApplicationContext(), TreatmentActivity.class);
            startActivity(pp);
            finish();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(this);
    }

    private void loadAnimalData() {
        try {
            adapterForSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            adapterForSpinner.add("Select Animal");
            android.database.sqlite.SQLiteDatabase db = getDb();
            if (db == null) return;

            Cursor cursor = db.rawQuery("SELECT id,name FROM animas WHERE lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = ''", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    adapterForSpinner.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }
            if (cursor != null) cursor.close();
            spinneranimal.setAdapter(adapterForSpinner);
        } catch (Exception e) {
            Log.v("IllnessActivity", e.getMessage());
        }
    }

    private void loadDiseasesData() {
        try {
            adapterForSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            adapterForSpinner.add("Select Disease Suspected");
            android.database.sqlite.SQLiteDatabase db = getDb();
            if (db == null) return;

            Cursor cursor = db.rawQuery("SELECT id,disease_name FROM diseases", null);
            int preSelectIndex = -1;
            int currentIndex = 1;
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String dName = cursor.getString(1);
                    adapterForSpinner.add(dName);
                    if (preSelectedType != null && !preSelectedType.isEmpty()) {
                        String cleanPre = preSelectedType.toLowerCase().trim();
                        String cleanDb = dName.toLowerCase().trim();
                        if (cleanDb.equalsIgnoreCase(cleanPre)
                                || cleanDb.contains(cleanPre)
                                || cleanPre.contains(cleanDb)) {
                            preSelectIndex = currentIndex;
                        }
                    }
                    currentIndex++;
                } while (cursor.moveToNext());
            }
            if (cursor != null) cursor.close();

            adapterForSpinner.add("Other Disease (Specify)...");
            spinnerdisease.setAdapter(adapterForSpinner);
            if (preSelectIndex != -1) {
                spinnerdisease.setSelection(preSelectIndex);
            }
        } catch (Exception e) {
            Log.v("IllnessActivity", e.getMessage());
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
