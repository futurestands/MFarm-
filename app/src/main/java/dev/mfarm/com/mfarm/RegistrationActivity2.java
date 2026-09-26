package dev.mfarm.com.mfarm;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Calendar;

public class RegistrationActivity2 extends AppCompatActivity {
    Button btndem, btnSire, btnsave;
    EditText edtbirthdate, edtweaningdate, edtbirthweight, edtweaningweight;

    private String animalName = "";
    private String breedId = "1";
    private String bodyConf = "";
    private String gender = "Female";
    private String bodyColor = "";
    private String dob = "";
    private String damId = "1";
    private String sireId = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration3);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        if (intent != null) {
            animalName = intent.getStringExtra("animal_name");
            breedId = intent.getStringExtra("breed_id");
            bodyConf = intent.getStringExtra("body_conf");
            gender = intent.getStringExtra("gender");
            bodyColor = intent.getStringExtra("body_color");
            dob = intent.getStringExtra("dob");
        }
        if (animalName == null) animalName = GlobalVariables.animal_name;
        if (breedId == null) breedId = GlobalVariables.breed_id;
        if (bodyConf == null) bodyConf = GlobalVariables.body_conf;
        if (gender == null) gender = GlobalVariables.gender;
        if (bodyColor == null) bodyColor = GlobalVariables.body_color;
        if (dob == null) dob = GlobalVariables.dob;

        btnSire = findViewById(R.id.btnSire);
        btndem = findViewById(R.id.btndem);
        btnsave = findViewById(R.id.btnsave);
        edtbirthdate = findViewById(R.id.edtbirthdate);
        edtweaningdate = findViewById(R.id.edtweaningdate);
        edtbirthweight = findViewById(R.id.edtbirhtweight);
        edtweaningweight = findViewById(R.id.edtweaningweight);

        View.OnClickListener dateClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText target = (EditText) v;
                Calendar calendar = Calendar.getInstance();
                new DatePickerDialog(RegistrationActivity2.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        target.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        };

        edtbirthdate.setOnClickListener(dateClickListener);
        edtweaningdate.setOnClickListener(dateClickListener);

        btndem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog("Dam ID", true);
            }
        });

        btnSire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog("Sire ID", false);
            }
        });

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent x = new Intent(getApplicationContext(), PhotoIntentActivity.class);
                x.putExtra("animal_name", animalName);
                x.putExtra("breed_id", breedId);
                x.putExtra("body_conf", bodyConf);
                x.putExtra("gender", gender);
                x.putExtra("body_color", bodyColor);
                x.putExtra("dob", dob);
                x.putExtra("birth_date", edtbirthdate.getText().toString());
                x.putExtra("birth_weight", edtbirthweight.getText().toString());
                x.putExtra("weaning_date", edtweaningdate.getText().toString());
                x.putExtra("weaning_weight", edtweaningweight.getText().toString());
                x.putExtra("dam_id", damId);
                x.putExtra("sire_id", sireId);
                startActivity(x);
            }
        });
    }

    protected void showInputDialog(final String title, final boolean isDam) {
        LayoutInflater layoutInflater = LayoutInflater.from(RegistrationActivity2.this);
        View promptView = layoutInflater.inflate(R.layout.idinflate, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RegistrationActivity2.this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setTitle(title);

        final EditText edtdamId = promptView.findViewById(R.id.edtdamId);
        edtdamId.setHint(title);

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String val = edtdamId.getText().toString().trim();
                        if (isDam) {
                            damId = val;
                            GlobalVariables.dam_id = val;
                            btndem.setText("Dam ID: " + val);
                        } else {
                            sireId = val;
                            GlobalVariables.sire_id = val;
                            btnSire.setText("Sire ID: " + val);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
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
