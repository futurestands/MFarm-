package dev.mfarm.com.mfarm;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.adapters.IllnessAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;
import dev.mfarm.com.mfarm.models.illness;

public class TreatmentActivity extends AppCompatActivity {
    List<illness> supplierList = new ArrayList<illness>();
    IllnessAdapter aAdpt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treatment);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Record Treatment");
        }

        final ListView lv = findViewById(R.id.listView);
        lv.setBackgroundColor(Color.TRANSPARENT);

        initList();

        aAdpt = new IllnessAdapter(this, supplierList);
        lv.setAdapter(aAdpt);

        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                showTreatmentDialog(aAdpt.getItem(position));
            }
        });
    }

    private void showTreatmentDialog(final illness item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Treatment for " + item.getAnimal_name());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        final EditText etDiagnosis = new EditText(this);
        etDiagnosis.setHint("Diagnosis");
        layout.addView(etDiagnosis);

        final EditText etTreatment = new EditText(this);
        etTreatment.setHint("Treatment / Procedures");
        layout.addView(etTreatment);

        final EditText etMedicine = new EditText(this);
        etMedicine.setHint("Medicine used");
        layout.addView(etMedicine);

        final EditText etVetName = new EditText(this);
        etVetName.setHint("Veterinarian Name");
        layout.addView(etVetName);

        final EditText etCost = new EditText(this);
        etCost.setHint("Cost (e.g. 50.00)");
        etCost.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etCost);

        builder.setView(layout);

        builder.setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String diagnosis = etDiagnosis.getText().toString().trim();
                String treatment = etTreatment.getText().toString().trim();
                String medicine = etMedicine.getText().toString().trim();
                String vetName = etVetName.getText().toString().trim();
                String costStr = etCost.getText().toString().trim();
                double cost = costStr.isEmpty() ? 0 : Double.parseDouble(costStr);
                String todayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());

                android.database.sqlite.SQLiteDatabase db = dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(TreatmentActivity.this);
                if (db == null) return;

                db.beginTransaction();
                try {
                    ContentValues values = new ContentValues();
                    values.put("diagnosis", diagnosis);
                    values.put("treatment", treatment);
                    values.put("medicine", medicine);
                    values.put("vet_name", vetName);
                    values.put("cost", cost);
                    values.put("treatment_date", todayDate);

                    db.update("illness", values, "id=?", new String[]{item.getId()});

                    // Atomic expense creation if cost > 0
                    if (cost > 0) {
                        ContentValues expValues = new ContentValues();
                        expValues.put("category", "Veterinary");
                        expValues.put("amount", cost);
                        expValues.put("date", todayDate);
                        expValues.put("description", "Treatment for " + item.getAnimal_name() + ": " + (treatment.isEmpty() ? diagnosis : treatment));
                        if (item.getAnimal_id() != null && !item.getAnimal_id().isEmpty()) {
                            try {
                                expValues.put("related_animal_id", Integer.parseInt(item.getAnimal_id()));
                            } catch (Exception ignored) {}
                        }
                        db.insert("expenses", null, expValues);
                    }

                    db.setTransactionSuccessful();
                    DatabaseHelper.logAudit(db, "RECORD_TREATMENT", "HEALTH", Long.parseLong(item.getId()), "Treatment recorded for " + item.getAnimal_name());
                    Toast.makeText(TreatmentActivity.this, "Treatment saved successfully", Toast.LENGTH_SHORT).show();

                    initList();
                    aAdpt.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(TreatmentActivity.this, "Error saving treatment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    db.endTransaction();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void initList() {
        supplierList.clear();
        android.database.sqlite.SQLiteDatabase db = dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor c = db.rawQuery("SELECT id, animal_id, animal_name, illness_occured, sings_noted, date_occured, sync_datetime, treatment, diagnosis, medicine, treatment_date, others, medicine_quantity, pregnancy_status, comments FROM illness ORDER BY id DESC", null);
        if (c != null && c.moveToFirst()) {
            do {
                supplierList.add(new illness(
                        c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                        c.getString(4), c.getString(5), c.getString(6), c.getString(7),
                        c.getString(8), c.getString(9), c.getString(10), c.getString(11),
                        c.getString(12), c.getString(13), c.getString(14)
                ));
            } while (c.moveToNext());
        }
        if (c != null) c.close();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
