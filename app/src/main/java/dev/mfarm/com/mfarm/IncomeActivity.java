package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.CategoryManager;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class IncomeActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerAnimal;
    private com.google.android.material.textfield.TextInputEditText etAmount, etBuyer, etDescription;
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
                DatePickerDialog dpd = new DatePickerDialog(IncomeActivity.this, R.style.GreenDatePickerTheme, new DatePickerDialog.OnDateSetListener() {
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

        View layoutCustomCategory = findViewById(R.id.layoutCustomCategory);
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

                if (layoutCustomCategory != null) {
                    if (CategoryManager.OTHER_SENTINEL.equals(cat)) {
                        layoutCustomCategory.setVisibility(View.VISIBLE);
                    } else {
                        layoutCustomCategory.setVisibility(View.GONE);
                    }
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
        btnDate.setText("📅 Date: " + dateFormatter.format(calendar.getTime()));
    }

    private void setupCategories() {
        android.database.sqlite.SQLiteDatabase db = getDb();
        DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
        String[] defaultCat = {"Milk Sales", "Animal Sales", "Crop Sales", "Egg Sales", "Breeding Services"};
        List<String> categories = CategoryManager.getCategories(adapterDb, "income_categories", "category_name", defaultCat);

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

        Cursor cursor = db.rawQuery("SELECT id, name FROM animas WHERE lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = ''", null);
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
        com.google.android.material.textfield.TextInputEditText etCustomCat = findViewById(R.id.etCustomCategory);
        if (CategoryManager.OTHER_SENTINEL.equals(category)) {
            if (etCustomCat != null && !etCustomCat.getText().toString().trim().isEmpty()) {
                category = etCustomCat.getText().toString().trim();
            } else {
                Toast.makeText(this, "Please specify custom income category name", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String amountStr = etAmount.getText().toString().trim();
        String buyer = etBuyer.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = dateFormatter.format(calendar.getTime());

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter income amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (!Double.isFinite(amount) || amount <= 0) {
                Toast.makeText(this, "Please enter a valid positive amount", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
            return;
        }

        int animalPos = spinnerAnimal.getSelectedItemPosition();
        int relatedAnimalId = (animalPos >= 0 && animalPos < animalIds.size()) ? animalIds.get(animalPos) : -1;

        if ("Animal Sales".equalsIgnoreCase(category) && relatedAnimalId == -1) {
            Toast.makeText(this, "Please select the animal being sold", Toast.LENGTH_SHORT).show();
            return;
        }

        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = false;
        String errorMessage = null;

        db.beginTransaction();
        try {
            DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
            CategoryManager.CategoryResult catRes = CategoryManager.addCategory(adapterDb, "income_categories", "category_name", category);
            if (catRes == CategoryManager.CategoryResult.FAILED && CategoryManager.OTHER_SENTINEL.equals(spinnerCategory.getSelectedItem().toString())) {
                throw new RuntimeException("Failed to persist custom income category");
            }

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
            if (rowId == -1) {
                throw new RuntimeException("Database insertion failed for income record");
            }

            if ("Animal Sales".equalsIgnoreCase(category) && relatedAnimalId != -1) {
                ContentValues statusValues = new ContentValues();
                statusValues.put("lifecycle_status", "Sold");
                int updatedRows = db.update("animas", statusValues,
                        "id=? AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')",
                        new String[]{String.valueOf(relatedAnimalId)});
                if (updatedRows != 1) {
                    throw new IllegalStateException("Animal ID " + relatedAnimalId + " is no longer active or has already been sold");
                }
            }

            DatabaseHelper.logAudit(db, "RECORD_INCOME", "FINANCIAL", rowId, "Income: " + category + " - " + amount);

            db.setTransactionSuccessful();
            success = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
        } finally {
            db.endTransaction();
        }

        if (success) {
            Toast.makeText(this, "Income Record Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error saving income: " + (errorMessage != null ? errorMessage : "Transaction failed"), Toast.LENGTH_LONG).show();
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
