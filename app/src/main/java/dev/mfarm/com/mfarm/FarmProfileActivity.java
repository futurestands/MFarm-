package dev.mfarm.com.mfarm;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class FarmProfileActivity extends AppCompatActivity {

    private TextInputEditText etFarmName, etOwnerName, etLocation, etPhone, etRegNumber, etCurrency, etFarmSize, etNotes;
    private Button btnSaveProfile;
    private long profileId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Farm Profile");
        }

        etFarmName = findViewById(R.id.etFarmName);
        etOwnerName = findViewById(R.id.etOwnerName);
        etLocation = findViewById(R.id.etLocation);
        etPhone = findViewById(R.id.etPhone);
        etRegNumber = findViewById(R.id.etRegNumber);
        etCurrency = findViewById(R.id.etCurrency);
        etFarmSize = findViewById(R.id.etFarmSize);
        etNotes = findViewById(R.id.etNotes);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        loadProfile();

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return DatabaseHelper.getDatabase(this);
    }

    private void loadProfile() {
        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;
        Cursor cursor = db.rawQuery("SELECT * FROM farm_profile LIMIT 1", null);
        if (cursor != null && cursor.moveToFirst()) {
            profileId = cursor.getLong(cursor.getColumnIndex("id"));
            etFarmName.setText(cursor.getString(cursor.getColumnIndex("farm_name")));
            etOwnerName.setText(cursor.getString(cursor.getColumnIndex("owner_name")));
            etLocation.setText(cursor.getString(cursor.getColumnIndex("location")));
            etPhone.setText(cursor.getString(cursor.getColumnIndex("phone")));
            etRegNumber.setText(cursor.getString(cursor.getColumnIndex("reg_number")));
            etCurrency.setText(cursor.getString(cursor.getColumnIndex("currency_symbol")));
            etFarmSize.setText(cursor.getString(cursor.getColumnIndex("farm_size")));
            etNotes.setText(cursor.getString(cursor.getColumnIndex("notes")));
        }
        if (cursor != null) cursor.close();
    }

    private void saveProfile() {
        String name = etFarmName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter farm name", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("farm_name", name);
        values.put("owner_name", etOwnerName.getText().toString().trim());
        values.put("location", etLocation.getText().toString().trim());
        values.put("phone", etPhone.getText().toString().trim());
        values.put("reg_number", etRegNumber.getText().toString().trim());
        values.put("currency_symbol", etCurrency.getText().toString().trim().isEmpty() ? "UGX" : etCurrency.getText().toString().trim());
        values.put("farm_size", etFarmSize.getText().toString().trim());
        values.put("notes", etNotes.getText().toString().trim());

        try {
            android.database.sqlite.SQLiteDatabase db = getDb();
            if (db != null) {
                if (profileId != -1) {
                    db.update("farm_profile", values, "id=?", new String[]{String.valueOf(profileId)});
                } else {
                    profileId = db.insert("farm_profile", null, values);
                }
                DatabaseHelper.logAudit(db, "UPDATE_PROFILE", "FARM_PROFILE", profileId, "Updated farm profile: " + name);
            }
            Toast.makeText(this, "Farm Profile Saved", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
