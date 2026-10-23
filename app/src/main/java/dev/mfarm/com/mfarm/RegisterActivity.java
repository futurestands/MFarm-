package dev.mfarm.com.mfarm;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.CategoryManager;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class RegisterActivity extends AppCompatActivity {
    Spinner spngender, spinnerSpecies, spnbreed;
    TextInputEditText edtname, edteartag, edtbodyconformance, edtbodycolor, edtdob;
    Button btnsave;

    String gender = "Female";
    String selectedSpecies = "Cattle";
    String breed_id = "1";
    String dob = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Individual Animal Registration");
        }

        spngender = findViewById(R.id.spngender);
        spinnerSpecies = findViewById(R.id.spinnerSpecies);
        spnbreed = findViewById(R.id.edtbreed);
        
        edtname = findViewById(R.id.edtname);
        edteartag = findViewById(R.id.edteartag);
        edtbodyconformance = findViewById(R.id.edtbodyconformance);
        edtbodycolor = findViewById(R.id.edtbodycolor);
        edtdob = findViewById(R.id.edtdob);
        btnsave = findViewById(R.id.btnsave);

        loadSpeciesSpinner();
        loadGenderSpinner();

        edtdob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        spinnerSpecies.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSpecies = parent.getItemAtPosition(position).toString();
                loadBreedSpinner(selectedSpecies);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spnbreed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String breed_name = parent.getItemAtPosition(position).toString();
                    android.database.sqlite.SQLiteDatabase db = getDb();
                    if (db != null) {
                        // First check old breeds table for cattle legacy compatibility
                        Cursor c = db.rawQuery("SELECT id FROM breeds WHERE name =?", new String[]{breed_name});
                        if (c != null && c.moveToFirst()) {
                            breed_id = c.getString(0);
                            c.close();
                        } else {
                            if (c != null) c.close();
                            // Fallback to species_breeds id
                            Cursor c2 = db.rawQuery("SELECT id FROM species_breeds WHERE breed_name =?", new String[]{breed_name});
                            if (c2 != null && c2.moveToFirst()) {
                                breed_id = c2.getString(0);
                            }
                            if (c2 != null) c2.close();
                        }
                    }
                } else {
                    breed_id = "1";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spngender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                gender = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tag = edteartag.getText().toString().trim();
                String name = edtname.getText().toString().trim();
                
                if (tag.isEmpty() && name.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter an Ear Tag or Animal Name", Toast.LENGTH_LONG).show();
                    edteartag.requestFocus();
                    return;
                }

                GlobalVariables.animal_name = name;
                GlobalVariables.breed_id = breed_id;
                GlobalVariables.body_conf = edtbodyconformance.getText().toString().trim();
                GlobalVariables.gender = gender;
                GlobalVariables.body_color = edtbodycolor.getText().toString().trim();
                GlobalVariables.dob = dob;

                Intent x = new Intent(getApplicationContext(), RegistrationActivity2.class);
                x.putExtra("animal_name", name);
                x.putExtra("ear_tag", tag);
                x.putExtra("species", selectedSpecies);
                x.putExtra("breed_id", breed_id);
                x.putExtra("body_conf", edtbodyconformance.getText().toString().trim());
                x.putExtra("gender", gender);
                x.putExtra("body_color", edtbodycolor.getText().toString().trim());
                x.putExtra("dob", dob);
                startActivity(x);
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                dob = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year);
                edtdob.setText(dob);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        
        dpd.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dpd.show();
    }

    private void loadGenderSpinner() {
        ArrayAdapter<String> adapterForSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapterForSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterForSpinner.add("Female");
        adapterForSpinner.add("Male");
        spngender.setAdapter(adapterForSpinner);
    }

    private void loadSpeciesSpinner() {
        List<String> species = new ArrayList<>();
        species.add("Cattle");
        species.add("Goat");
        species.add("Sheep");
        species.add("Rabbit");
        species.add("Poultry");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, species);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecies.setAdapter(adapter);
    }

    private void loadBreedSpinner(String speciesName) {
        ArrayAdapter<String> adapterForBreedSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapterForBreedSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterForBreedSpinner.add("Select Breed");

        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db != null) {
            Cursor cursor = null;
            if ("Cattle".equalsIgnoreCase(speciesName)) {
                // Use old breeds table for cattle backward compatibility
                cursor = db.rawQuery("SELECT id, name FROM breeds ORDER BY name ASC", null);
            } else {
                cursor = db.rawQuery("SELECT id, breed_name FROM species_breeds WHERE species_name = ? ORDER BY breed_name ASC", new String[]{speciesName});
            }
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    adapterForBreedSpinner.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }
            if (cursor != null) cursor.close();
        }
        spnbreed.setAdapter(adapterForBreedSpinner);
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(this);
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
