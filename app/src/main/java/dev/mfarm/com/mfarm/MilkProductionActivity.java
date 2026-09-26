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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class MilkProductionActivity extends AppCompatActivity {
    private Spinner spinneranimal;
    private Button btnsave, btndate;
    private EditText edtlitres, edtmilkCondition;
    private List<String> animalNames = new ArrayList<>();
    private List<Integer> animalIds = new ArrayList<>();
    private int animal_id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milk_production);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Record Milk Yield");
        }

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        spinneranimal = findViewById(R.id.spinner);
        btnsave = findViewById(R.id.btnsave);
        btndate = findViewById(R.id.btndate);
        edtlitres = findViewById(R.id.edtlitres);
        edtmilkCondition = findViewById(R.id.edtmilkCondition);

        btndate.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date()));
        btndate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        loadAnimalData();

        spinneranimal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < animalIds.size()) {
                    animal_id = animalIds.get(position);
                } else {
                    animal_id = 0;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                animal_id = 0;
            }
        });

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMilk();
            }
        });
    }

    private void saveMilk() {
        String litresStr = edtlitres.getText().toString().trim();
        if (litresStr.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter milk yield in litres", Toast.LENGTH_LONG).show();
            return;
        }

        if (animal_id == 0) {
            Toast.makeText(getApplicationContext(), "Please select an animal", Toast.LENGTH_LONG).show();
            return;
        }

        double litres = Double.parseDouble(litresStr);
        String date = btndate.getText().toString();

        MainActivity.database.beginTransaction();
        try {
            ContentValues collect = new ContentValues();
            collect.put("litres", litres);
            collect.put("animal_id", animal_id);
            collect.put("description", edtmilkCondition.getText().toString().trim());
            collect.put("datetime", date);

            long rowId = MainActivity.database.insert("milk_production", null, collect);
            if (rowId != -1) {
                MainActivity.database.setTransactionSuccessful();
                DatabaseHelper.logAudit(MainActivity.database, "RECORD_MILK", "MILK_PRODUCTION", rowId, "Recorded " + litres + "L for Animal ID: " + animal_id);
                Toast.makeText(getApplicationContext(), "Saved Successfully", Toast.LENGTH_LONG).show();

                Intent pp = new Intent(getApplicationContext(), MainActivity.class);
                pp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(pp);
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to save milk record", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error saving milk yield: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            MainActivity.database.endTransaction();
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                btndate.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadAnimalData() {
        animalIds.clear();
        animalNames.clear();
        try {
            Cursor cursor = MainActivity.database.rawQuery("SELECT id, name FROM animas", null);
            if (cursor.moveToFirst()) {
                do {
                    animalIds.add(cursor.getInt(0));
                    animalNames.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (animalNames.isEmpty()) {
                animalNames.add("No Animals Registered");
            }

            ArrayAdapter<String> adapterForSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, animalNames);
            adapterForSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinneranimal.setAdapter(adapterForSpinner);
        } catch (Exception e) {
            Log.v("MilkProductionActivity", e.getMessage());
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
