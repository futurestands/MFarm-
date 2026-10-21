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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class DisposalActivity extends AppCompatActivity {

    private Spinner spinnerAnimal, spinnerMethod;
    private TextInputEditText etCause, etPrice, etBuyer, etNotes;
    private Button btnDate, btnSave;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> animalNames = new ArrayList<>();
    private List<Integer> animalIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disposal);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Record Animal Offtake / Disposal");
        }

        spinnerAnimal = findViewById(R.id.spinnerAnimal);
        spinnerMethod = findViewById(R.id.spinnerMethod);
        etCause = findViewById(R.id.etCause);
        etPrice = findViewById(R.id.etPrice);
        etBuyer = findViewById(R.id.etBuyer);
        etNotes = findViewById(R.id.etNotes);
        btnDate = findViewById(R.id.btnDate);
        btnSave = findViewById(R.id.btnSave);

        setupMethodSpinner();
        loadAnimals();
        updateDateButton();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(DisposalActivity.this, new DatePickerDialog.OnDateSetListener() {
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
                saveDisposal();
            }
        });
    }

    private void updateDateButton() {
        btnDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private void setupMethodSpinner() {
        String[] methods = {"Sold", "Death", "Culled", "Stolen", "Missing", "Slaughtered"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, methods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMethod.setAdapter(adapter);
    }

    private void loadAnimals() {
        animalIds.clear();
        animalNames.clear();

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT id, name, species FROM animas WHERE lifecycle_status = 'Active' OR lifecycle_status IS NULL ORDER BY name ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String species = cursor.getString(2);
                animalIds.add(id);
                animalNames.add(name + " (" + (species != null ? species : "Livestock") + ")");
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnimal.setAdapter(adapter);
    }

    private void saveDisposal() {
        int selectedPos = spinnerAnimal.getSelectedItemPosition();
        if (animalIds.isEmpty() || selectedPos < 0 || selectedPos >= animalIds.size()) {
            Toast.makeText(this, "Please select an animal", Toast.LENGTH_SHORT).show();
            return;
        }

        int animalId = animalIds.get(selectedPos);
        String animalName = animalNames.get(selectedPos);
        String method = spinnerMethod.getSelectedItem().toString();
        String cause = etCause.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        double price = priceStr.isEmpty() ? 0 : Double.parseDouble(priceStr);
        String buyer = etBuyer.getText().toString().trim();
        String dateStr = btnDate.getText().toString();
        String notes = etNotes.getText().toString().trim();

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        DatabaseAdapter adapter = new AndroidDatabaseAdapter(db);
        adapter.beginTransaction();
        try {
            Map<String, Object> dispVal = new HashMap<>();
            dispVal.put("animal_id", animalId);
            dispVal.put("animal_tag", animalName);
            dispVal.put("disposal_date", dateStr);
            dispVal.put("method", method);
            dispVal.put("cause_or_reason", cause);
            dispVal.put("price", price);
            dispVal.put("buyer_or_destination", buyer);
            dispVal.put("notes", notes);

            long recordId = adapter.insert("disposal_records", null, dispVal);

            // Update animal lifecycle_status
            String newStatus = "Sold".equalsIgnoreCase(method) ? "Sold" : ("Death".equalsIgnoreCase(method) || "Slaughtered".equalsIgnoreCase(method) ? "Died" : method);
            Map<String, Object> animVal = new HashMap<>();
            animVal.put("lifecycle_status", newStatus);
            adapter.update("animas", animVal, "id = ?", new String[]{String.valueOf(animalId)});

            // If price > 0 and method is Sold, log an Income record
            if (price > 0 && "Sold".equalsIgnoreCase(method)) {
                Map<String, Object> incVal = new HashMap<>();
                incVal.put("category", "Animal Sales");
                incVal.put("amount", price);
                incVal.put("buyer", buyer);
                incVal.put("date", dateStr);
                incVal.put("description", "Sale of " + animalName);
                incVal.put("related_animal_id", animalId);
                incVal.put("project_code", animalName.toLowerCase().contains("goat") ? "GT2" : "CW1");
                adapter.insert("income", null, incVal);
            }

            adapter.setTransactionSuccessful();
            DatabaseHelper.logAudit(db, "RECORD_DISPOSAL", "LIVESTOCK", recordId, "Disposed animal " + animalName + " via " + method);

            Toast.makeText(this, "Disposal Record Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving disposal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            adapter.endTransaction();
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
