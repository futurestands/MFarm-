package dev.mfarm.com.mfarm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnimalDoctorActivity extends AppCompatActivity {

    private static final String TAG = "AnimalDoctor";

    private TextInputEditText etSearchDisease;
    private Button btnOpenSymptomChecker, btnTogglePreventiveCare;
    private View layoutPreventiveCare;
    private TextView tvGuideHeader;
    private ListView lvDiseases;

    private List<VeterinaryKnowledgeBase.DiseaseGuide> masterGuideList = new ArrayList<>();
    private List<VeterinaryKnowledgeBase.DiseaseGuide> filteredGuideList = new ArrayList<>();
    private DiseaseAdapter adapter;

    private boolean showingSymptomResults = false;
    private final boolean[] selectedSymptomBooleans = new boolean[VeterinaryKnowledgeBase.ALL_FARMER_SYMPTOMS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_doctor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Veterinary Health Guide");
        }

        lvDiseases = findViewById(R.id.lvDiseases);

        // Inflate header view containing search bar, buttons, emergency cards, and section title
        LayoutInflater inflater = LayoutInflater.from(this);
        View headerView = inflater.inflate(R.layout.header_animal_doctor, lvDiseases, false);

        etSearchDisease = headerView.findViewById(R.id.etSearchDisease);
        btnOpenSymptomChecker = headerView.findViewById(R.id.btnOpenSymptomChecker);
        btnTogglePreventiveCare = headerView.findViewById(R.id.btnTogglePreventiveCare);
        layoutPreventiveCare = headerView.findViewById(R.id.layoutPreventiveCare);
        tvGuideHeader = headerView.findViewById(R.id.tvGuideHeader);

        // Emergency card clicks
        headerView.findViewById(R.id.btnEmergencyAnthrax).setOnClickListener(v -> openDetail("anthrax"));
        headerView.findViewById(R.id.btnEmergencyRabies).setOnClickListener(v -> openDetail("rabies"));
        headerView.findViewById(R.id.btnEmergencyFMD).setOnClickListener(v -> openDetail("fmd"));
        headerView.findViewById(R.id.btnEmergencyCBPP).setOnClickListener(v -> openDetail("cbpp"));

        // Attach header to ListView BEFORE setAdapter
        lvDiseases.addHeaderView(headerView);

        // Load disease data
        List<VeterinaryKnowledgeBase.DiseaseGuide> allDiseases = VeterinaryKnowledgeBase.getAllDiseases();
        Log.d(TAG, "Diagnostic Log - VeterinaryKnowledgeBase.getAllDiseases().size(): " + allDiseases.size());
        if (!allDiseases.isEmpty()) {
            Log.d(TAG, "Diagnostic Log - First Disease ID: " + allDiseases.get(0).id + ", Name: " + allDiseases.get(0).name);
        }

        masterGuideList.addAll(allDiseases);
        filteredGuideList.addAll(masterGuideList);

        adapter = new DiseaseAdapter(this, filteredGuideList);
        lvDiseases.setAdapter(adapter);

        Log.d(TAG, "Diagnostic Log - masterGuideList size: " + masterGuideList.size());
        Log.d(TAG, "Diagnostic Log - filteredGuideList size: " + filteredGuideList.size());
        Log.d(TAG, "Diagnostic Log - lvDiseases null? " + (lvDiseases == null));
        Log.d(TAG, "Diagnostic Log - Adapter attached? " + (lvDiseases != null && lvDiseases.getAdapter() != null));

        // Search listener
        etSearchDisease.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (showingSymptomResults && s.length() > 0) {
                    showingSymptomResults = false;
                }
                filterDiseases(s.toString().trim().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Symptom Checker Dialog
        btnOpenSymptomChecker.setOnClickListener(v -> showSymptomCheckerDialog());

        // Preventive Care Toggle
        btnTogglePreventiveCare.setOnClickListener(v -> {
            if (layoutPreventiveCare.getVisibility() == View.VISIBLE) {
                layoutPreventiveCare.setVisibility(View.GONE);
                btnTogglePreventiveCare.setText("🛡️ PREVENTIVE CARE");
            } else {
                layoutPreventiveCare.setVisibility(View.VISIBLE);
                btnTogglePreventiveCare.setText("🛡️ HIDE PREVENTIVE CARE");
            }
        });

        // Tap item to open disease detail
        lvDiseases.setOnItemClickListener((parent, view1, position, id) -> {
            // Note: position 0 is the headerView, so adapter item is at (position - 1)
            int adapterPosition = position - lvDiseases.getHeaderViewsCount();
            if (adapterPosition >= 0 && adapterPosition < adapter.getCount()) {
                VeterinaryKnowledgeBase.DiseaseGuide item = adapter.getItem(adapterPosition);
                if (item != null) {
                    openDetail(item.id);
                }
            }
        });
    }

    private void filterDiseases(String query) {
        filteredGuideList.clear();
        if (query.isEmpty()) {
            filteredGuideList.addAll(masterGuideList);
            tvGuideHeader.setText("COMMON LIVESTOCK DISEASES & GUIDES");
        } else {
            for (VeterinaryKnowledgeBase.DiseaseGuide d : masterGuideList) {
                if (d.name.toLowerCase().contains(query)
                        || (d.aliases != null && d.aliases.toLowerCase().contains(query))
                        || d.cause.toLowerCase().contains(query)
                        || d.category.toLowerCase().contains(query)
                        || d.symptomsString().toLowerCase().contains(query)
                        || d.veterinaryManagement.toLowerCase().contains(query)) {
                    filteredGuideList.add(d);
                }
            }
            tvGuideHeader.setText("SEARCH RESULTS (" + filteredGuideList.size() + " MATCHES)");
        }
        adapter.notifyDataSetChanged();
    }

    private void showSymptomCheckerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Observed Symptoms");

        builder.setMultiChoiceItems(VeterinaryKnowledgeBase.ALL_FARMER_SYMPTOMS, selectedSymptomBooleans,
                (dialog, which, isChecked) -> selectedSymptomBooleans[which] = isChecked);

        builder.setPositiveButton("Check Possible Conditions", (dialog, which) -> {
            Set<String> selectedSet = new HashSet<>();
            for (int i = 0; i < selectedSymptomBooleans.length; i++) {
                if (selectedSymptomBooleans[i]) {
                    selectedSet.add(VeterinaryKnowledgeBase.ALL_FARMER_SYMPTOMS[i]);
                }
            }

            if (selectedSet.isEmpty()) {
                Toast.makeText(AnimalDoctorActivity.this, "Please select at least one symptom", Toast.LENGTH_SHORT).show();
                return;
            }

            List<VeterinaryKnowledgeBase.SymptomMatch> matches = VeterinaryKnowledgeBase.evaluateSymptoms(selectedSet);
            filteredGuideList.clear();
            for (VeterinaryKnowledgeBase.SymptomMatch m : matches) {
                filteredGuideList.add(m.disease);
            }

            showingSymptomResults = true;
            tvGuideHeader.setText("POSSIBLE CONDITIONS TO CONSIDER (" + matches.size() + " MATCHES)");
            adapter.notifyDataSetChanged();

            Toast.makeText(AnimalDoctorActivity.this,
                    "Possible conditions to consider based on " + selectedSet.size() + " sign(s). Veterinary examination may be required.",
                    Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Clear / Reset", (dialog, which) -> {
            for (int i = 0; i < selectedSymptomBooleans.length; i++) {
                selectedSymptomBooleans[i] = false;
            }
            filteredGuideList.clear();
            filteredGuideList.addAll(masterGuideList);
            showingSymptomResults = false;
            tvGuideHeader.setText("COMMON LIVESTOCK DISEASES & GUIDES");
            adapter.notifyDataSetChanged();
        });

        builder.show();
    }

    private void openDetail(String diseaseId) {
        Intent intent = new Intent(AnimalDoctorActivity.this, DiseaseDetailActivity.class);
        intent.putExtra("disease_id", diseaseId);
        startActivity(intent);
    }

    private class DiseaseAdapter extends ArrayAdapter<VeterinaryKnowledgeBase.DiseaseGuide> {
        public DiseaseAdapter(Context context, List<VeterinaryKnowledgeBase.DiseaseGuide> list) {
            super(context, R.layout.disease_row_layout, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.disease_row_layout, parent, false);
            }

            final VeterinaryKnowledgeBase.DiseaseGuide item = getItem(position);
            if (item != null) {
                TextView tvName = convertView.findViewById(R.id.tvDiseaseName);
                TextView tvBadge = convertView.findViewById(R.id.tvUrgencyBadge);
                TextView tvSymptoms = convertView.findViewById(R.id.tvSymptoms);
                TextView tvAction = convertView.findViewById(R.id.tvTreatmentAction);
                Button btnLog = convertView.findViewById(R.id.btnLogThisIllness);

                tvName.setText(item.name);
                tvBadge.setText(item.urgency);
                if ("CRITICAL".equalsIgnoreCase(item.urgency)) {
                    tvBadge.setBackgroundColor(Color.parseColor("#B71C1C"));
                } else if ("HIGH".equalsIgnoreCase(item.urgency)) {
                    tvBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
                } else {
                    tvBadge.setBackgroundColor(Color.parseColor("#F57C00"));
                }

                tvSymptoms.setText("Clinical Signs: " + String.join(", ", item.clinicalSigns));
                tvAction.setText("Action: " + (item.immediateActions.isEmpty() ? item.prevention : item.immediateActions.get(0)));

                btnLog.setOnClickListener(v -> openDetail(item.id));
            }

            return convertView;
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
