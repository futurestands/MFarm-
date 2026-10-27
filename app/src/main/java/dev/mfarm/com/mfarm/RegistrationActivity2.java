package dev.mfarm.com.mfarm;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class RegistrationActivity2 extends AppCompatActivity {
    Button btndem, btnSire, btnsave;

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
            getSupportActionBar().setTitle("Parentage & Photo");
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

        btndem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog("Dam Details", true);
            }
        });

        btnSire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog("Sire Details", false);
            }
        });

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent x = new Intent(getApplicationContext(), PhotoIntentActivity.class);
                x.putExtra("animal_name", animalName);
                x.putExtra("ear_tag", getIntent().getStringExtra("ear_tag"));
                x.putExtra("species", getIntent().getStringExtra("species"));
                x.putExtra("breed_id", breedId);
                x.putExtra("body_conf", bodyConf);
                x.putExtra("gender", gender);
                x.putExtra("body_color", bodyColor);
                x.putExtra("dob", dob);
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
        final EditText edtparentbreed = promptView.findViewById(R.id.edtparentbreed);
        final EditText edtbreedpercentage = promptView.findViewById(R.id.edtbreedpercentage);

        edtdamId.setHint(isDam ? "Dam ID or Tag Number" : "Sire ID or Tag Number");

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String idVal = edtdamId.getText().toString().trim();
                        String breedVal = edtparentbreed != null ? edtparentbreed.getText().toString().trim() : "";
                        String pctVal = edtbreedpercentage != null ? edtbreedpercentage.getText().toString().trim() : "";

                        StringBuilder fullDetail = new StringBuilder(idVal.isEmpty() ? "N/A" : idVal);
                        if (!breedVal.isEmpty()) {
                            fullDetail.append(" (").append(breedVal);
                            if (!pctVal.isEmpty()) {
                                fullDetail.append(" ").append(pctVal).append("%");
                            }
                            fullDetail.append(")");
                        } else if (!pctVal.isEmpty()) {
                            fullDetail.append(" (").append(pctVal).append("%)");
                        }

                        String resultVal = fullDetail.toString();
                        if (isDam) {
                            damId = resultVal;
                            GlobalVariables.dam_id = resultVal;
                            btndem.setText("Dam: " + resultVal);
                        } else {
                            sireId = resultVal;
                            GlobalVariables.sire_id = resultVal;
                            btnSire.setText("Sire: " + resultVal);
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
