package dev.mfarm.com.mfarm;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class AnimalDetailActivity extends AppCompatActivity {

    private String animalId;
    private ImageView ivAnimalPhoto;
    private TextView tvAnimalName, tvBreed, tvGender, tvDob, tvBodyConf, tvDam, tvSire;
    private TextView tvLifecycleStatus, tvReproStatus, tvLactationStatus, tvHealthStatus, tvHistorySummary;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_detail);

        animalId = getIntent().getStringExtra("animal_id");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Animal Details");
        }

        ivAnimalPhoto = findViewById(R.id.ivAnimalPhoto);
        tvAnimalName = findViewById(R.id.tvAnimalName);
        tvBreed = findViewById(R.id.tvBreed);
        tvGender = findViewById(R.id.tvGender);
        tvDob = findViewById(R.id.tvDob);
        tvBodyConf = findViewById(R.id.tvBodyConf);
        tvDam = findViewById(R.id.tvDam);
        tvSire = findViewById(R.id.tvSire);
        tvLifecycleStatus = findViewById(R.id.tvLifecycleStatus);
        tvReproStatus = findViewById(R.id.tvReproStatus);
        tvLactationStatus = findViewById(R.id.tvLactationStatus);
        tvHealthStatus = findViewById(R.id.tvHealthStatus);
        tvHistorySummary = findViewById(R.id.tvHistorySummary);
        btnDelete = findViewById(R.id.btnDelete);

        loadDetails();
        loadHistorySummary();

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void loadDetails() {
        String sql = "SELECT a.*, b.name as breed_name FROM animas a " +
                     "LEFT JOIN breeds b ON a.breed_id = b.id " +
                     "WHERE a.id = ?";
        Cursor cursor = MainActivity.database.rawQuery(sql, new String[]{animalId});

        if (cursor.moveToFirst()) {
            tvAnimalName.setText(cursor.getString(cursor.getColumnIndex("name")));
            tvBreed.setText(cursor.getString(cursor.getColumnIndex("breed_name")));

            String genderStr = cursor.getString(cursor.getColumnIndex("gender"));
            if ("1".equals(genderStr)) genderStr = "Female";
            else if ("0".equals(genderStr)) genderStr = "Male";
            tvGender.setText(genderStr != null ? genderStr : "Female");

            String dobStr = cursor.getString(cursor.getColumnIndex("dob"));
            if ("1".equals(dobStr) || "0".equals(dobStr)) dobStr = "Not specified";
            tvDob.setText(dobStr != null && !dobStr.isEmpty() ? dobStr : "Not specified");

            tvBodyConf.setText(cursor.getString(cursor.getColumnIndex("body_conf")));
            tvDam.setText(cursor.getString(cursor.getColumnIndex("dam_id")));
            tvSire.setText(cursor.getString(cursor.getColumnIndex("sire_id")));

            int idxLife = cursor.getColumnIndex("lifecycle_status");
            int idxRepro = cursor.getColumnIndex("repro_status");
            int idxLact = cursor.getColumnIndex("lactation_status");
            int idxHealth = cursor.getColumnIndex("health_status");

            if (idxLife >= 0 && !cursor.isNull(idxLife)) tvLifecycleStatus.setText(cursor.getString(idxLife));
            if (idxRepro >= 0 && !cursor.isNull(idxRepro)) tvReproStatus.setText(cursor.getString(idxRepro));
            if (idxLact >= 0 && !cursor.isNull(idxLact)) tvLactationStatus.setText(cursor.getString(idxLact));
            if (idxHealth >= 0 && !cursor.isNull(idxHealth)) tvHealthStatus.setText(cursor.getString(idxHealth));

            String photoPath = cursor.getString(cursor.getColumnIndex("photo_path"));
            if (photoPath != null && !photoPath.isEmpty()) {
                File imgFile = new File(photoPath);
                if (imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    ivAnimalPhoto.setImageBitmap(myBitmap);
                }
            }
        }
        cursor.close();
    }

    private void loadHistorySummary() {
        StringBuilder sb = new StringBuilder();

        // Vaccinations
        Cursor c1 = MainActivity.database.rawQuery("SELECT vaccine_name, scheduled_date, status FROM vaccinations WHERE animal_id = ? ORDER BY id DESC LIMIT 3", new String[]{animalId});
        sb.append("• Vaccinations:\n");
        if (c1.moveToFirst()) {
            do {
                sb.append("   - ").append(c1.getString(0)).append(" on ").append(c1.getString(1)).append(" [").append(c1.getString(2)).append("]\n");
            } while (c1.moveToNext());
        } else {
            sb.append("   - No vaccination records.\n");
        }
        c1.close();

        // Health
        Cursor c2 = MainActivity.database.rawQuery("SELECT illness_occured, date_occured, diagnosis FROM illness WHERE animal_id = ? ORDER BY id DESC LIMIT 3", new String[]{animalId});
        sb.append("\n• Health Events:\n");
        if (c2.moveToFirst()) {
            do {
                sb.append("   - ").append(c2.getString(0)).append(" on ").append(c2.getString(1));
                if (c2.getString(2) != null && !c2.getString(2).isEmpty()) {
                    sb.append(" (").append(c2.getString(2)).append(")");
                }
                sb.append("\n");
            } while (c2.moveToNext());
        } else {
            sb.append("   - No illness records.\n");
        }
        c2.close();

        // Breeding
        Cursor c3 = MainActivity.database.rawQuery("SELECT mating_date, expected_birth_date, status FROM breeding_records WHERE animal_id = ? ORDER BY id DESC LIMIT 3", new String[]{animalId});
        sb.append("\n• Breeding Records:\n");
        if (c3.moveToFirst()) {
            do {
                sb.append("   - Mated: ").append(c3.getString(0)).append(" | Expected: ").append(c3.getString(1)).append(" [").append(c3.getString(2)).append("]\n");
            } while (c3.moveToNext());
        } else {
            sb.append("   - No breeding records.\n");
        }
        c3.close();

        tvHistorySummary.setText(sb.toString());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this animal record?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.database.beginTransaction();
                        try {
                            // Find and cancel all pending vaccination alarms for this animal
                            Cursor cursor = MainActivity.database.rawQuery(
                                    "SELECT id FROM vaccinations WHERE animal_id = ?",
                                    new String[]{animalId});
                            if (cursor != null) {
                                while (cursor.moveToNext()) {
                                    int vacId = cursor.getInt(0);
                                    AlarmScheduler.cancel(AnimalDetailActivity.this, vacId);
                                    AlarmScheduler.clearNotified(AnimalDetailActivity.this, vacId);
                                }
                                cursor.close();
                            }
                            MainActivity.database.delete("vaccinations", "animal_id=?", new String[]{animalId});
                            MainActivity.database.delete("illness", "animal_id=?", new String[]{animalId});
                            MainActivity.database.delete("milk_production", "animal_id=?", new String[]{animalId});
                            MainActivity.database.delete("breeding_records", "animal_id=?", new String[]{animalId});
                            MainActivity.database.delete("vet_checks", "animal_id=?", new String[]{animalId});

                            ContentValues detachIncome = new ContentValues();
                            detachIncome.putNull("related_animal_id");
                            MainActivity.database.update("income", detachIncome, "related_animal_id=?", new String[]{animalId});

                            ContentValues detachExpense = new ContentValues();
                            detachExpense.putNull("related_animal_id");
                            MainActivity.database.update("expenses", detachExpense, "related_animal_id=?", new String[]{animalId});

                            MainActivity.database.delete("animas", "id=?", new String[]{animalId});
                            MainActivity.database.setTransactionSuccessful();
                            DatabaseHelper.logAudit(MainActivity.database, "DELETE_ANIMAL", "ANIMALS", Long.parseLong(animalId), "Deleted animal record ID: " + animalId);
                            Toast.makeText(AnimalDetailActivity.this, "Record deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(AnimalDetailActivity.this, "Error deleting record", Toast.LENGTH_SHORT).show();
                        } finally {
                            MainActivity.database.endTransaction();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
