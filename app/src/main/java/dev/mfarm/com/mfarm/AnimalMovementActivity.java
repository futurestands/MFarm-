package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
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

public class AnimalMovementActivity extends AppCompatActivity {

    private Spinner spinnerAnimal, spinnerFromLocation, spinnerToLocation;
    private TextInputEditText etReason, etMovedBy;
    private Button btnDate, btnSaveTransfer;
    private ListView lvMovementHistory;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> animalNames = new ArrayList<>();
    private List<Integer> animalIds = new ArrayList<>();
    private List<String> movementLogs = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_movement);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Farm Location Transfer & Movement");
        }

        spinnerAnimal = findViewById(R.id.spinnerAnimal);
        spinnerFromLocation = findViewById(R.id.spinnerFromLocation);
        spinnerToLocation = findViewById(R.id.spinnerToLocation);
        etReason = findViewById(R.id.etReason);
        etMovedBy = findViewById(R.id.etMovedBy);
        btnDate = findViewById(R.id.btnDate);
        btnSaveTransfer = findViewById(R.id.btnSaveTransfer);
        lvMovementHistory = findViewById(R.id.lvMovementHistory);

        setupLocationSpinners();
        loadAnimals();
        updateDateButton();
        loadMovementHistory();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(AnimalMovementActivity.this, new DatePickerDialog.OnDateSetListener() {
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

        btnSaveTransfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransfer();
            }
        });
    }

    private void updateDateButton() {
        btnDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private void setupLocationSpinners() {
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
        spinnerFromLocation.setAdapter(locAdapter);
        spinnerToLocation.setAdapter(locAdapter);
        if (locations.size() > 1) {
            spinnerToLocation.setSelection(1);
        }
    }

    private void loadAnimals() {
        animalIds.clear();
        animalNames.clear();

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT id, name, species, location_name FROM animas WHERE lifecycle_status = 'Active' OR lifecycle_status IS NULL ORDER BY name ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String species = cursor.getString(2);
                String loc = cursor.getString(3);
                animalIds.add(id);
                animalNames.add(name + " (" + (species != null ? species : "Livestock") + " @ " + (loc != null ? loc : "Main Farm") + ")");
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnimal.setAdapter(adapter);
    }

    private void loadMovementHistory() {
        movementLogs.clear();
        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT animal_name, from_location, to_location, movement_date, reason FROM animal_movements ORDER BY id DESC LIMIT 50", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String fromLoc = cursor.getString(1);
                String toLoc = cursor.getString(2);
                String dateStr = cursor.getString(3);
                String reason = cursor.getString(4);

                movementLogs.add(dateStr + " — " + name + "\n" + fromLoc + " ➔ " + toLoc + (reason != null && !reason.isEmpty() ? " (" + reason + ")" : ""));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        if (movementLogs.isEmpty()) {
            movementLogs.add("No location transfers recorded yet.");
        }

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movementLogs);
        lvMovementHistory.setAdapter(listAdapter);
    }

    private void saveTransfer() {
        int selectedPos = spinnerAnimal.getSelectedItemPosition();
        if (animalIds.isEmpty() || selectedPos < 0 || selectedPos >= animalIds.size()) {
            Toast.makeText(this, "Please select an animal", Toast.LENGTH_SHORT).show();
            return;
        }

        int animalId = animalIds.get(selectedPos);
        String animalName = animalNames.get(selectedPos);
        String fromLoc = spinnerFromLocation.getSelectedItem().toString();
        String toLoc = spinnerToLocation.getSelectedItem().toString();
        String reason = etReason.getText().toString().trim();
        String movedBy = etMovedBy.getText().toString().trim();
        String dateStr = btnDate.getText().toString();

        if (fromLoc.equals(toLoc)) {
            Toast.makeText(this, "Destination location must be different from source location", Toast.LENGTH_SHORT).show();
            return;
        }

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        DatabaseAdapter adapter = new AndroidDatabaseAdapter(db);
        adapter.beginTransaction();
        try {
            Map<String, Object> moveVal = new HashMap<>();
            moveVal.put("animal_id", animalId);
            moveVal.put("animal_name", animalName);
            moveVal.put("from_location", fromLoc);
            moveVal.put("to_location", toLoc);
            moveVal.put("movement_date", dateStr);
            moveVal.put("reason", reason);
            moveVal.put("moved_by", movedBy);

            long recordId = adapter.insert("animal_movements", null, moveVal);

            // Update animal's active location
            Map<String, Object> animVal = new HashMap<>();
            animVal.put("location_name", toLoc);
            adapter.update("animas", animVal, "id = ?", new String[]{String.valueOf(animalId)});

            adapter.setTransactionSuccessful();
            DatabaseHelper.logAudit(db, "TRANSFER_ANIMAL", "LOCATION", recordId, "Transferred " + animalName + " from " + fromLoc + " to " + toLoc);

            Toast.makeText(this, "Animal Transferred Successfully", Toast.LENGTH_SHORT).show();
            etReason.setText("");
            etMovedBy.setText("");

            loadAnimals();
            loadMovementHistory();
        } catch (Exception e) {
            Toast.makeText(this, "Error transferring animal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
