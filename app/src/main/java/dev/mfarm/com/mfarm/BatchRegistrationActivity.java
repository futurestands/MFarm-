package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.BatchManager;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class BatchRegistrationActivity extends AppCompatActivity {

    private Spinner spinnerSpecies, spinnerSubtype, spinnerLocation;
    private TextInputEditText etBatchId, etBreed, etPurpose, etQuantity, etAgeWeeks, etSupplier, etCost, etNotes;
    private Button btnDate, btnSaveBatch;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_registration);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Group & Batch Registration");
        }

        spinnerSpecies = findViewById(R.id.spinnerSpecies);
        spinnerSubtype = findViewById(R.id.spinnerSubtype);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        etBatchId = findViewById(R.id.etBatchId);
        etBreed = findViewById(R.id.etBreed);
        etPurpose = findViewById(R.id.etPurpose);
        etQuantity = findViewById(R.id.etQuantity);
        etAgeWeeks = findViewById(R.id.etAgeWeeks);
        etSupplier = findViewById(R.id.etSupplier);
        etCost = findViewById(R.id.etCost);
        etNotes = findViewById(R.id.etNotes);
        btnDate = findViewById(R.id.btnDate);
        btnSaveBatch = findViewById(R.id.btnSaveBatch);

        generateDefaultBatchId();
        setupSpinners();
        updateDateButton();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(BatchRegistrationActivity.this, new DatePickerDialog.OnDateSetListener() {
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

        spinnerSpecies.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSubtypes(spinnerSpecies.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSaveBatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBatch();
            }
        });
    }

    private void generateDefaultBatchId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US);
        etBatchId.setText("BATCH-" + sdf.format(new Date()));
    }

    private void updateDateButton() {
        btnDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private void setupSpinners() {
        String[] speciesArr = {"Poultry", "Rabbit", "Goat", "Sheep", "Cattle"};
        ArrayAdapter<String> specAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, speciesArr);
        specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecies.setAdapter(specAdapter);

        // Populate locations
        List<String> locations = new ArrayList<>();
        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db != null) {
            Cursor cursor = db.rawQuery("SELECT location_name FROM farm_locations ORDER BY id ASC", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    locations.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            if (cursor != null) cursor.close();
        }
        if (locations.isEmpty()) {
            locations.add("Main Farm");
            locations.add("Kicwamba");
            locations.add("Nkesiga");
        }
        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locAdapter);
    }

    private void updateSubtypes(String species) {
        String[] subtypes;
        if ("Poultry".equalsIgnoreCase(species)) {
            subtypes = new String[]{"Day-Old Chicks", "Broilers", "Layers", "Ducklings", "Poult Turkeys", "Growers"};
        } else if ("Rabbit".equalsIgnoreCase(species)) {
            subtypes = new String[]{"Grower Rabbits", "Fattening Rabbits", "Breeding Group", "Kits"};
        } else {
            subtypes = new String[]{"Young Group", "Commercial Group", "Fattening Group"};
        }
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subtypes);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubtype.setAdapter(subAdapter);
    }

    private void saveBatch() {
        String batchId = etBatchId.getText().toString().trim();
        String species = spinnerSpecies.getSelectedItem().toString();
        String subtype = spinnerSubtype.getSelectedItem().toString();
        String location = spinnerLocation.getSelectedItem().toString();
        String breed = etBreed.getText().toString().trim();
        String purpose = etPurpose.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String ageStr = etAgeWeeks.getText().toString().trim();
        String supplier = etSupplier.getText().toString().trim();
        String costStr = etCost.getText().toString().trim();
        String dateStr = btnDate.getText().toString();
        String notes = etNotes.getText().toString().trim();

        if (batchId.isEmpty()) {
            Toast.makeText(this, "Please enter a Batch ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Please enter initial batch quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        double ageWeeks = ageStr.isEmpty() ? 0 : Double.parseDouble(ageStr);
        double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        DatabaseAdapter adapter = new AndroidDatabaseAdapter(db);
        try {
            long rowId = BatchManager.createBatch(adapter, batchId, species, subtype, breed, purpose, qty, dateStr, ageWeeks, supplier, location, "", cost, notes);
            DatabaseHelper.logAudit(db, "CREATE_BATCH", "LIVESTOCK_BATCH", rowId, "Registered batch " + batchId + " (" + qty + " " + subtype + ")");

            Toast.makeText(this, "Batch Registered Successfully! Quantity: " + qty, Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving batch: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
