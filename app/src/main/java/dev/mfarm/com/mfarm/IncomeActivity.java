package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class IncomeActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerAnimal;
    private TextInputEditText etAmount, etBuyer, etDescription;
    private Button btnDate, btnSaveIncome;
    private TextView tvAnimalLabel;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> animalNames = new ArrayList<>();
    private List<Integer> animalIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Record Income");
        }

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerAnimal = findViewById(R.id.spinnerAnimal);
        etAmount = findViewById(R.id.etAmount);
        etBuyer = findViewById(R.id.etBuyer);
        etDescription = findViewById(R.id.etDescription);
        btnDate = findViewById(R.id.btnDate);
        btnSaveIncome = findViewById(R.id.btnSaveIncome);
        tvAnimalLabel = findViewById(R.id.tvAnimalLabel);

        setupCategories();
        loadAnimals();
        updateDateButton();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(IncomeActivity.this, new DatePickerDialog.OnDateSetListener() {
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

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String cat = parent.getItemAtPosition(position).toString();
                if ("Animal Sales".equalsIgnoreCase(cat)) {
                    tvAnimalLabel.setVisibility(View.VISIBLE);
                    spinnerAnimal.setVisibility(View.VISIBLE);
                } else {
                    tvAnimalLabel.setVisibility(View.GONE);
                    spinnerAnimal.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSaveIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveIncome();
            }
        });
    }

    private void updateDateButton() {
        btnDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private void setupCategories() {
        String[] categories = {"Milk Sales", "Animal Sales", "Manure Sales", "Breeding Services", "Crop Sales", "Other Income"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return DatabaseHelper.getDatabase(this);
    }

    private void loadAnimals() {
        animalIds.clear();
        animalNames.clear();

        animalIds.add(-1);
        animalNames.add("None / Not Applicable");

        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT id, name FROM animas WHERE lifecycle_status = 'Active'", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                animalIds.add(cursor.getInt(0));
                animalNames.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnimal.setAdapter(adapter);
    }

    private void saveIncome() {
        String category = spinnerCategory.getSelectedItem().toString();
        String amountStr = etAmount.getText().toString().trim();
        String buyer = etBuyer.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = btnDate.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        int animalPos = spinnerAnimal.getSelectedItemPosition();
        int relatedAnimalId = (animalPos >= 0 && animalPos < animalIds.size()) ? animalIds.get(animalPos) : -1;

        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("category", category);
            values.put("amount", amount);
            values.put("buyer", buyer);
            values.put("date", date);
            values.put("description", description);
            if (relatedAnimalId != -1) {
                values.put("related_animal_id", relatedAnimalId);
            }

            long rowId = db.insert("income", null, values);

            if ("Animal Sales".equalsIgnoreCase(category) && relatedAnimalId != -1) {
                ContentValues statusValues = new ContentValues();
                statusValues.put("lifecycle_status", "Sold");
                db.update("animas", statusValues, "id=?", new String[]{String.valueOf(relatedAnimalId)});
            }

            db.setTransactionSuccessful();
            DatabaseHelper.logAudit(db, "RECORD_INCOME", "FINANCIAL", rowId, "Income: " + category + " - " + amount);

            Toast.makeText(this, "Income Record Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving income: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
