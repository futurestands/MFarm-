package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
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

public class VetCheckActivity extends AppCompatActivity {

    private Spinner spinnerAnimals;
    private TextInputEditText etCheckType, etRemarks;
    private Button btnCheckDate, btnSave;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> animalNames = new ArrayList<>();
    private List<Integer> animalIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_check);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Vet Check Record");
        }

        spinnerAnimals = findViewById(R.id.spinnerAnimals);
        etCheckType = findViewById(R.id.etCheckType);
        etRemarks = findViewById(R.id.etRemarks);
        btnCheckDate = findViewById(R.id.btnCheckDate);
        btnSave = findViewById(R.id.btnSave);

        loadAnimals();
        updateDateButton();

        btnCheckDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(VetCheckActivity.this, new DatePickerDialog.OnDateSetListener() {
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

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRecord();
            }
        });
    }

    private void updateDateButton() {
        btnCheckDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return DatabaseHelper.getDatabase(this);
    }

    private void loadAnimals() {
        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT id, name FROM animas", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                animalIds.add(cursor.getInt(0));
                animalNames.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnimals.setAdapter(adapter);
    }

    private void saveRecord() {
        int selectedPos = spinnerAnimals.getSelectedItemPosition();
        if (selectedPos == -1) {
            Toast.makeText(this, "Please select an animal", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = etCheckType.getText().toString().trim();
        String remarks = etRemarks.getText().toString().trim();
        if (type.isEmpty()) {
            Toast.makeText(this, "Please enter check type", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("animal_id", animalIds.get(selectedPos));
        values.put("check_type", type);
        values.put("check_date", btnCheckDate.getText().toString());
        values.put("remarks", remarks);

        try {
            android.database.sqlite.SQLiteDatabase db = getDb();
            if (db != null) {
                db.insert("vet_checks", null, values);
            }
            Toast.makeText(this, "Vet Check Saved Successfully", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
