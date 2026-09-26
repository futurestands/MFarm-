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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class IllnessActivity extends AppCompatActivity {
    Spinner spinneranimal, spinnerdisease;
    private ArrayAdapter<String> adapterForSpinner;
    String animalName = "";

    int animal_id = 0;
    EditText edtsymptoms;
    Button btnSave, btndate;
    String disease = "";
    String dateOccured = "";
    String preSelectedType = "";

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
        edtsymptoms = findViewById(R.id.edtsymptoms);
        btnSave = findViewById(R.id.btnSave);
        btndate = findViewById(R.id.btndate);

        if (getIntent().hasExtra("type")) {
            preSelectedType = getIntent().getStringExtra("type");
        }

        btndate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                new DatePickerDialog(IllnessActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        dateOccured = dayOfMonth + "-" + (month + 1) + "-" + year;
                        btndate.setText(dateOccured);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        loadAnimalData();
        loadDiseasesData();
        spinneranimal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                animalName = ((TextView) view).getText().toString();
                Cursor cursor = MainActivity.database.rawQuery("SELECT id,name FROM animas where name = ?", new String[]{animalName});
                if (cursor.moveToFirst()) {
                    animal_id = cursor.getInt(0);
                }
                cursor.close();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerdisease.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                disease = ((TextView) view).getText().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtsymptoms.getText().toString().equalsIgnoreCase("")) {
                    Toast.makeText(getApplicationContext(), "Please Enter description", Toast.LENGTH_LONG).show();
                } else if (animal_id == 0) {
                    Toast.makeText(getApplicationContext(), "Please Select Animal", Toast.LENGTH_LONG).show();
                } else {
                    if (dateOccured.isEmpty() || dateOccured.equalsIgnoreCase("Select Date Occurred")) {
                        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                        dateOccured = df.format(Calendar.getInstance().getTime());
                    }

                    ContentValues collect = new ContentValues();
                    collect.put("animal_id", animal_id);
                    collect.put("animal_name", animalName);
                    collect.put("sings_noted", edtsymptoms.getText().toString());
                    collect.put("illness_occured", disease);
                    collect.put("treatment", "");
                    collect.put("date_occured", dateOccured);
                    collect.put("diagnosis", "");
                    collect.put("medicine", "");
                    collect.put("treatment_date", "");
                    collect.put("others", "");

                    try {
                        MainActivity.database.insert("illness", null, collect);
                        Toast.makeText(getApplicationContext(), "Saved Successfully", Toast.LENGTH_LONG).show();

                        Intent pp = new Intent(getApplicationContext(), TreatmentActivity.class);
                        startActivity(pp);
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void loadAnimalData() {
        try {
            adapterForSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            adapterForSpinner.add("Select Animal");
            Cursor cursor = MainActivity.database.rawQuery("SELECT id,name FROM animas", null);
            if (cursor.moveToFirst()) {
                do {
                    adapterForSpinner.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }
            cursor.close();
            spinneranimal.setAdapter(adapterForSpinner);
        } catch (Exception e) {
            Log.v("IllnessActivity", e.getMessage());
        }
    }

    private void loadDiseasesData() {
        try {
            adapterForSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            adapterForSpinner.add("Select Disease Suspected");
            Cursor cursor = MainActivity.database.rawQuery("SELECT id,disease_name FROM diseases", null);
            int preSelectIndex = -1;
            int currentIndex = 1;
            if (cursor.moveToFirst()) {
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
            cursor.close();
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
