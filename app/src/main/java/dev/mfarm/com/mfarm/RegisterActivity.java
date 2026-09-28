package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity {
    Spinner spngender;
    private ArrayAdapter<String> adapterForSpinner;
    Button btnsave, btndate;
    EditText edtname, edtbodyconformance, edtbodycolor;
    Spinner spnbreed;
    String breed_id = "1";
    String gender = "Female";
    String dob = "";

    private ArrayAdapter<String> adapterForBreedSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spngender = findViewById(R.id.spngender);
        LoadSpinnerGender();
        edtname = findViewById(R.id.edtname);
        spnbreed = findViewById(R.id.edtbreed);
        LoadBreedSpinner();
        edtbodyconformance = findViewById(R.id.edtbodyconformance);
        edtbodycolor = findViewById(R.id.edtbodycolor);
        btndate = findViewById(R.id.btndate);

        btndate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        spnbreed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String breed_name = parent.getItemAtPosition(position).toString();
                    android.database.sqlite.SQLiteDatabase db = getDb();
                    if (db != null) {
                        Cursor cursor = db.rawQuery("SELECT id FROM breeds WHERE name =?", new String[]{breed_name});
                        if (cursor != null && cursor.moveToFirst()) {
                            breed_id = cursor.getString(0);
                        }
                        if (cursor != null) cursor.close();
                    }
                } else {
                    breed_id = "1";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spngender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                gender = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnsave = findViewById(R.id.btnsave);
        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edtname.getText().toString().trim();
                if (name.length() < 2) {
                    Toast.makeText(getApplicationContext(), "Please enter animal name", Toast.LENGTH_LONG).show();
                    edtname.requestFocus();
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
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                dob = dayOfMonth + "-" + (month + 1) + "-" + year;
                btndate.setText(dob);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void LoadSpinnerGender() {
        try {
            adapterForSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            adapterForSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapterForSpinner.add("Female");
            adapterForSpinner.add("Male");
            spngender.setAdapter(adapterForSpinner);
        } catch (Exception e) {
            Log.v("RegisterActivity", e.getMessage());
        }
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(this);
    }

    private void LoadBreedSpinner() {
        try {
            adapterForBreedSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            adapterForBreedSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapterForBreedSpinner.add("Select Breed");

            android.database.sqlite.SQLiteDatabase db = getDb();
            if (db == null) return;

            Cursor cursor = db.rawQuery("SELECT id, name FROM breeds", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    adapterForBreedSpinner.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }
            if (cursor != null) cursor.close();
            spnbreed.setAdapter(adapterForBreedSpinner);
        } catch (Exception e) {
            Log.v("RegisterActivity", e.getMessage());
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
