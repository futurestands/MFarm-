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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.CategoryManager;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class CropManagementActivity extends AppCompatActivity {

    private Spinner spinnerCropType, spinnerActivityType;
    private View layoutCustomCrop;
    private TextInputLayout tilInputsUsed, tilQuantity, tilCost, tilYieldHarvested;
    private TextInputEditText etCustomCrop, etPlotName, etInputsUsed, etQuantity, etCost, etYieldHarvested, etNotes;
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
        layoutCustomCrop = findViewById(R.id.layoutCustomCrop);
        etCustomCrop = findViewById(R.id.etCustomCrop);

        tilInputsUsed = findViewById(R.id.tilInputsUsed);
        tilQuantity = findViewById(R.id.tilQuantity);
        tilCost = findViewById(R.id.tilCost);
        tilYieldHarvested = findViewById(R.id.tilYieldHarvested);

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
                DatePickerDialog dpd = new DatePickerDialog(CropManagementActivity.this, R.style.GreenDatePickerTheme, new DatePickerDialog.OnDateSetListener() {
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

        btnSaveActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCropActivity();
            }
        });
    }

    private void updateDateButton() {
        btnDate.setText("📅 Date: " + dateFormatter.format(calendar.getTime()));
    }

    private void setupSpinners() {
        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
        String[] defaultCrops = {"Coffee (CF3)", "Groundnuts", "Maize", "Beans"};
        List<String> crops = CategoryManager.getCategories(adapterDb, "crop_types", "crop_name", defaultCrops);

        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, crops);
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCropType.setAdapter(cropAdapter);

        spinnerCropType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String cName = parent.getItemAtPosition(position).toString();
                if (layoutCustomCrop != null) {
                    if (CategoryManager.OTHER_SENTINEL.equals(cName)) {
                        layoutCustomCrop.setVisibility(View.VISIBLE);
                    } else {
                        layoutCustomCrop.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] activities = {"Planting", "Fertilizing", "Spraying", "Weeding", "Labor", "Harvesting", "Crop Sale"};
        ArrayAdapter<String> actAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activities);
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityType.setAdapter(actAdapter);

        spinnerActivityType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String act = parent.getItemAtPosition(position).toString();
                updateFieldVisibilitiesForActivity(act);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateFieldVisibilitiesForActivity(String actType) {
        if (tilInputsUsed == null || tilQuantity == null || tilCost == null || tilYieldHarvested == null) return;

        if ("Harvesting".equalsIgnoreCase(actType)) {
            // Harvesting: Hide inputs, show yield harvested and labor cost
            tilInputsUsed.setVisibility(View.GONE);
            tilQuantity.setVisibility(View.GONE);
            tilCost.setVisibility(View.VISIBLE);
            tilCost.setHint("Harvesting Labor / Transport Cost (UGX)");
            tilYieldHarvested.setVisibility(View.VISIBLE);
            tilYieldHarvested.setHint("Harvest Yield (Kg / Bags)");
        } else if ("Weeding".equalsIgnoreCase(actType) || "Labor".equalsIgnoreCase(actType)) {
            // Weeding / Labor: Hide harvest yield and quantity used
            tilInputsUsed.setVisibility(View.VISIBLE);
            tilInputsUsed.setHint("Labor / Equipment Used (Optional)");
            tilQuantity.setVisibility(View.GONE);
            tilCost.setVisibility(View.VISIBLE);
            tilCost.setHint("Weeding / Labor Cost (UGX)");
            tilYieldHarvested.setVisibility(View.GONE);
        } else if ("Planting".equalsIgnoreCase(actType)) {
            // Planting: Show seeds used, seed quantity, planting cost
            tilInputsUsed.setVisibility(View.VISIBLE);
            tilInputsUsed.setHint("Seed / Variety Used (e.g. Longe 5 Maize)");
            tilQuantity.setVisibility(View.VISIBLE);
            tilQuantity.setHint("Seed Quantity Used (Kg)");
            tilCost.setVisibility(View.VISIBLE);
            tilCost.setHint("Seed Cost / Planting Labor (UGX)");
            tilYieldHarvested.setVisibility(View.GONE);
        } else if ("Fertilizing".equalsIgnoreCase(actType)) {
            // Fertilizing: Show fertilizer name, quantity, cost
            tilInputsUsed.setVisibility(View.VISIBLE);
            tilInputsUsed.setHint("Fertilizer / Manure Name (e.g. NPK)");
            tilQuantity.setVisibility(View.VISIBLE);
            tilQuantity.setHint("Fertilizer Quantity Used (Kg / Bags)");
            tilCost.setVisibility(View.VISIBLE);
            tilCost.setHint("Fertilizer Cost / Application Labor (UGX)");
            tilYieldHarvested.setVisibility(View.GONE);
        } else if ("Spraying".equalsIgnoreCase(actType)) {
            // Spraying: Show chemical name, volume, cost
            tilInputsUsed.setVisibility(View.VISIBLE);
            tilInputsUsed.setHint("Pesticide / Fungicide Name");
            tilQuantity.setVisibility(View.VISIBLE);
            tilQuantity.setHint("Spray Volume / Quantity Used (L / ml)");
            tilCost.setVisibility(View.VISIBLE);
            tilCost.setHint("Chemical Cost / Spraying Labor (UGX)");
            tilYieldHarvested.setVisibility(View.GONE);
        } else if ("Crop Sale".equalsIgnoreCase(actType)) {
            // Crop Sale: Show buyer name, quantity sold, sale revenue
            tilInputsUsed.setVisibility(View.VISIBLE);
            tilInputsUsed.setHint("Buyer / Market Name (Optional)");
            tilQuantity.setVisibility(View.VISIBLE);
            tilQuantity.setHint("Quantity Sold (Kg / Bags)");
            tilCost.setVisibility(View.VISIBLE);
            tilCost.setHint("Sale Revenue Received (UGX)");
            tilYieldHarvested.setVisibility(View.GONE);
        } else {
            // Default / Other
            tilInputsUsed.setVisibility(View.VISIBLE);
            tilInputsUsed.setHint("Inputs / Materials Used");
            tilQuantity.setVisibility(View.VISIBLE);
            tilQuantity.setHint("Quantity Used");
            tilCost.setVisibility(View.VISIBLE);
            tilCost.setHint("Cost / Revenue (UGX)");
            tilYieldHarvested.setVisibility(View.VISIBLE);
            tilYieldHarvested.setHint("Harvest Yield (Kg)");
        }
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
        if (CategoryManager.OTHER_SENTINEL.equals(cropType)) {
            if (etCustomCrop != null && !etCustomCrop.getText().toString().trim().isEmpty()) {
                cropType = etCustomCrop.getText().toString().trim();
            } else {
                Toast.makeText(this, "Please specify custom crop name", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String actType = spinnerActivityType.getSelectedItem().toString();
        String plotName = etPlotName.getText().toString().trim();
        String inputs = etInputsUsed.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        double qty = qtyStr.isEmpty() ? 0 : Double.parseDouble(qtyStr);
        String costStr = etCost.getText().toString().trim();
        double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
        String yieldStr = etYieldHarvested.getText().toString().trim();
        double yieldVal = yieldStr.isEmpty() ? 0 : Double.parseDouble(yieldStr);
        String dateStr = dateFormatter.format(calendar.getTime());
        String notes = etNotes.getText().toString().trim();

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = false;
        String errorMessage = null;

        db.beginTransaction();
        try {
            DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
            CategoryManager.addCategory(adapterDb, "crop_types", "crop_name", cropType);

            Map<String, Object> actVal = new HashMap<>();
            actVal.put("activity_type", actType + " (" + cropType + ")");
            actVal.put("activity_date", dateStr);
            actVal.put("inputs_used", (plotName.isEmpty() ? "" : "Plot: " + plotName + ". ") + inputs);
            actVal.put("quantity", qty);
            actVal.put("cost", cost);
            actVal.put("yield_harvested", yieldVal);
            actVal.put("notes", notes);

            long rowId = adapterDb.insert("crop_activities", null, actVal);
            if (rowId == -1) {
                throw new RuntimeException("Failed to insert crop activity record");
            }

            // Log Expense if cost > 0 and not a Crop Sale
            if (cost > 0 && !"Crop Sale".equalsIgnoreCase(actType)) {
                Map<String, Object> expVal = new HashMap<>();
                expVal.put("category", "Crop Inputs & Labor");
                expVal.put("amount", cost);
                expVal.put("date", dateStr);
                expVal.put("description", actType + " for " + cropType + (plotName.isEmpty() ? "" : " (" + plotName + ")"));
                expVal.put("project_code", resolveEnterpriseCode(cropType));
                adapterDb.insert("expenses", null, expVal);
            }

            // Log Income if Crop Sale
            if ("Crop Sale".equalsIgnoreCase(actType) && cost > 0) {
                Map<String, Object> incVal = new HashMap<>();
                incVal.put("category", "Crop Sales");
                incVal.put("amount", cost);
                incVal.put("date", dateStr);
                incVal.put("description", "Sale of " + cropType + (yieldVal > 0 ? " (" + yieldVal + " kg)" : ""));
                incVal.put("project_code", resolveEnterpriseCode(cropType));
                adapterDb.insert("income", null, incVal);
            }

            db.setTransactionSuccessful();
            DatabaseHelper.logAudit(db, "RECORD_CROP_ACTIVITY", "CROPS", rowId, actType + " recorded for " + cropType);
            success = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
        } finally {
            db.endTransaction();
        }

        if (success) {
            Toast.makeText(this, "Crop Activity Saved Successfully", Toast.LENGTH_SHORT).show();
            etInputsUsed.setText("");
            etQuantity.setText("");
            etCost.setText("");
            etYieldHarvested.setText("");
            etNotes.setText("");

            loadActivities();
        } else {
            Toast.makeText(this, "Error saving activity: " + (errorMessage != null ? errorMessage : "Transaction failed"), Toast.LENGTH_LONG).show();
        }
    }

    private String resolveEnterpriseCode(String cropType) {
        if (cropType == null || cropType.trim().isEmpty()) return "GEN";
        String clean = cropType.trim();
        String upper = clean.toUpperCase();
        if (upper.contains("COFFEE") || upper.contains("CF3")) return "CF3";
        if (upper.contains("MAIZE") || upper.contains("MZ6")) return "MZ6";
        if (upper.contains("BEAN") || upper.contains("BN7")) return "BN7";
        if (upper.contains("GROUNDNUT") || upper.contains("PEANUT") || upper.contains("GN8")) return "GN8";

        // Query by exact enterprise name first to prevent truncation collision
        String entName = clean + " Enterprise";
        try {
            android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
            if (db != null) {
                Cursor c = db.rawQuery("SELECT code FROM enterprises WHERE LOWER(name) = LOWER(?)", new String[]{entName});
                if (c != null && c.moveToFirst()) {
                    String existingCode = c.getString(0);
                    c.close();
                    return existingCode;
                }
                if (c != null) c.close();

                // Generate a unique non-colliding code using hash
                String sanitized = clean.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                int hashCode = Math.abs(clean.toLowerCase().hashCode()) % 10000;
                String customCode = "CP_" + (sanitized.length() > 6 ? sanitized.substring(0, 6) : sanitized) + "_" + hashCode;

                ContentValues cv = new ContentValues();
                cv.put("code", customCode);
                cv.put("name", entName);
                cv.put("category", "Crop");
                cv.put("description", "Auto-generated enterprise for " + clean);
                db.insert("enterprises", null, cv);
                return customCode;
            }
        } catch (Exception ignored) {}

        return "GEN";
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
