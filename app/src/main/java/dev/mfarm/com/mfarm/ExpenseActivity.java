package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.ContentValues;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.CategoryManager;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class ExpenseActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private View layoutCustomCategory;
    private TextInputEditText etCategory, etAmount, etDescription;
    private Button btnDate, btnSave;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Expense");
        }

        spinnerCategory = findViewById(R.id.spinnerCategory);
        layoutCustomCategory = findViewById(R.id.layoutCustomCategory);
        etCategory = findViewById(R.id.etCategory);
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        btnDate = findViewById(R.id.btnDate);
        btnSave = findViewById(R.id.btnSave);

        setupCategories();
        updateDateButton();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });
    }

    private void setupCategories() {
        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
        String[] defaultCat = {"Feeds & Supplements", "Veterinary & Medicine", "Labor & Wages", "Equipment & Tools", "Seeds & Fertilizer", "Transport"};
        List<String> categories = CategoryManager.getCategories(adapterDb, "expense_categories", "category_name", defaultCat);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String cat = parent.getItemAtPosition(position).toString();
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
    }

    private void showDatePicker() {
        DatePickerDialog dpd = new DatePickerDialog(this, R.style.GreenDatePickerTheme, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateButton();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Date Constraint: Expense date cannot be in the future
        dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
        dpd.show();
    }

    private void updateDateButton() {
        btnDate.setText("📅 Date: " + dateFormatter.format(calendar.getTime()));
    }

    private void saveExpense() {
        String category = spinnerCategory.getSelectedItem().toString();
        if (CategoryManager.OTHER_SENTINEL.equals(category)) {
            if (etCategory != null && !etCategory.getText().toString().trim().isEmpty()) {
                category = etCategory.getText().toString().trim();
            } else {
                Toast.makeText(this, "Please specify custom expense category name", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = dateFormatter.format(calendar.getTime());

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter expense amount", Toast.LENGTH_SHORT).show();
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

        ContentValues values = new ContentValues();
        values.put("category", category);
        values.put("amount", amount);
        values.put("date", date);
        values.put("description", description);

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        db.beginTransaction();
        try {
            DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
            CategoryManager.CategoryResult catRes = CategoryManager.addCategory(adapterDb, "expense_categories", "category_name", category);
            if (catRes == CategoryManager.CategoryResult.FAILED && CategoryManager.OTHER_SENTINEL.equals(spinnerCategory.getSelectedItem().toString())) {
                throw new RuntimeException("Failed to persist custom expense category");
            }

            long rowId = db.insert("expenses", null, values);
            if (rowId == -1) {
                throw new RuntimeException("Database insertion failed for expense record");
            }

            DatabaseHelper.logAudit(db, "RECORD_EXPENSE", "FINANCIAL", rowId, "Expense: " + category + " - " + amount);

            db.setTransactionSuccessful();
            Toast.makeText(this, "Expense Saved Successfully", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving expense: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
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
