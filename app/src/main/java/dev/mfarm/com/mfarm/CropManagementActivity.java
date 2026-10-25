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

public class CropManagementActivity extends AppCompatActivity {

    private Spinner spinnerCropType, spinnerActivityType;
    private TextInputEditText etPlotName, etInputsUsed, etQuantity, etCost, etYieldHarvested, etNotes;
    private Button btnDate, btnSaveActivity;
    private ListView lvCropActivities;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
    private List<String> activityLogs = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Crop & Enterprise Management");
        }

        spinnerCropType = findViewById(R.id.spinnerCropType);
        spinnerActivityType = findViewById(R.id.spinnerActivityType);
        etPlotName = findViewById(R.id.etPlotName);
        etInputsUsed = findViewById(R.id.etInputsUsed);
        etQuantity = findViewById(R.id.etQuantity);
        etCost = findViewById(R.id.etCost);
        etYieldHarvested = findViewById(R.id.etYieldHarvested);
        etNotes = findViewById(R.id.etNotes);
        btnDate = findViewById(R.id.btnDate);
        btnSaveActivity = findViewById(R.id.btnSaveActivity);
        lvCropActivities = findViewById(R.id.lvCropActivities);

        setupSpinners();
        updateDateButton();
        loadActivities();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(CropManagementActivity.this, new DatePickerDialog.OnDateSetListener() {
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

        btnSaveActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCropActivity();
            }
        });
    }

    private void updateDateButton() {
        btnDate.setText(dateFormatter.format(calendar.getTime()));
    }

    private void setupSpinners() {
        String[] crops = {"Coffee (CF3)", "Groundnuts", "Maize", "Beans", "Other Crops"};
        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, crops);
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCropType.setAdapter(cropAdapter);

        String[] activities = {"Planting", "Fertilizing", "Spraying", "Weeding", "Labor", "Harvesting", "Crop Sale"};
        ArrayAdapter<String> actAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activities);
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityType.setAdapter(actAdapter);
    }

    private void loadActivities() {
        activityLogs.clear();
        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT activity_date, activity_type, inputs_used, cost, yield_harvested, notes FROM crop_activities ORDER BY id DESC LIMIT 50", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String dateStr = cursor.getString(0);
                String type = cursor.getString(1);
                String inputs = cursor.getString(2);
                double cost = cursor.getDouble(3);
                double yieldVal = cursor.getDouble(4);
                String notes = cursor.getString(5);

                String log = dateStr + " [" + type + "]\n" + (inputs != null && !inputs.isEmpty() ? "Inputs: " + inputs + " | " : "") +
                        (cost > 0 ? "Cost: UGX " + String.format(Locale.US, "%,.0f", cost) + " | " : "") +
                        (yieldVal > 0 ? "Harvest: " + yieldVal + " kg | " : "") + (notes != null ? notes : "");
                activityLogs.add(log);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        if (activityLogs.isEmpty()) {
            activityLogs.add("No crop activities recorded yet.");
        }

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, activityLogs);
        lvCropActivities.setAdapter(listAdapter);
    }

    private void saveCropActivity() {
        String cropType = spinnerCropType.getSelectedItem().toString();
        String actType = spinnerActivityType.getSelectedItem().toString();
        String plotName = etPlotName.getText().toString().trim();
        String inputs = etInputsUsed.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        double qty = qtyStr.isEmpty() ? 0 : Double.parseDouble(qtyStr);
        String costStr = etCost.getText().toString().trim();
        double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
        String yieldStr = etYieldHarvested.getText().toString().trim();
        double yieldVal = yieldStr.isEmpty() ? 0 : Double.parseDouble(yieldStr);
        String dateStr = btnDate.getText().toString();
        String notes = etNotes.getText().toString().trim();

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        DatabaseAdapter adapter = new AndroidDatabaseAdapter(db);
        adapter.beginTransaction();
        try {
            Map<String, Object> actVal = new HashMap<>();
            actVal.put("activity_type", actType + " (" + cropType + ")");
            actVal.put("activity_date", dateStr);
            actVal.put("inputs_used", (plotName.isEmpty() ? "" : "Plot: " + plotName + ". ") + inputs);
            actVal.put("quantity", qty);
            actVal.put("cost", cost);
            actVal.put("yield_harvested", yieldVal);
            actVal.put("notes", notes);

            long rowId = adapter.insert("crop_activities", null, actVal);

            // Log Expense if cost > 0
            if (cost > 0 && !"Crop Sale".equalsIgnoreCase(actType)) {
                Map<String, Object> expVal = new HashMap<>();
                expVal.put("category", "Crop Inputs & Labor");
                expVal.put("amount", cost);
                expVal.put("date", dateStr);
                expVal.put("description", actType + " for " + cropType + (plotName.isEmpty() ? "" : " (" + plotName + ")"));
                expVal.put("project_code", resolveEnterpriseCode(cropType));
                adapter.insert("expenses", null, expVal);
            }

            // Log Income if Crop Sale
            if ("Crop Sale".equalsIgnoreCase(actType) && cost > 0) {
                Map<String, Object> incVal = new HashMap<>();
                incVal.put("category", "Crop Sales");
                incVal.put("amount", cost);
                incVal.put("date", dateStr);
                incVal.put("description", "Sale of " + cropType + (yieldVal > 0 ? " (" + yieldVal + " kg)" : ""));
                incVal.put("project_code", resolveEnterpriseCode(cropType));
                adapter.insert("income", null, incVal);
            }

            adapter.setTransactionSuccessful();
            DatabaseHelper.logAudit(db, "RECORD_CROP_ACTIVITY", "CROPS", rowId, actType + " recorded for " + cropType);

            Toast.makeText(this, "Crop Activity Saved Successfully", Toast.LENGTH_SHORT).show();
            etInputsUsed.setText("");
            etQuantity.setText("");
            etCost.setText("");
            etYieldHarvested.setText("");
            etNotes.setText("");

            loadActivities();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            adapter.endTransaction();
        }
    }

    private String resolveEnterpriseCode(String cropType) {
        if (cropType != null && cropType.toUpperCase().contains("CF3")) return "CF3";
        return "GEN"; // General enterprise if not coffee
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
