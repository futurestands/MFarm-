package dev.mfarm.com.mfarm;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DiseaseDetailActivity extends AppCompatActivity {

    private TextView tvDetailTitle, tvDetailAliases, tvDetailUrgencyBadge, tvDetailZoonoticBadge, tvDetailNotifiableBadge;
    private TextView tvOverviewContent, tvSignsContent, tvFarmerActionsContent, tvVetManagementContent, tvPreventionSourcesContent;
    private Button btnDetailRecordIllness, btnDetailRecordTreatment, btnDetailViewHistory;

    private VeterinaryKnowledgeBase.DiseaseGuide guide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String diseaseId = getIntent().getStringExtra("disease_id");
        if (diseaseId == null && getIntent().hasExtra("disease_name")) {
            diseaseId = getIntent().getStringExtra("disease_name");
        }

        guide = VeterinaryKnowledgeBase.getById(diseaseId);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailAliases = findViewById(R.id.tvDetailAliases);
        tvDetailUrgencyBadge = findViewById(R.id.tvDetailUrgencyBadge);
        tvDetailZoonoticBadge = findViewById(R.id.tvDetailZoonoticBadge);
        tvDetailNotifiableBadge = findViewById(R.id.tvDetailNotifiableBadge);

        tvOverviewContent = findViewById(R.id.tvOverviewContent);
        tvSignsContent = findViewById(R.id.tvSignsContent);
        tvFarmerActionsContent = findViewById(R.id.tvFarmerActionsContent);
        tvVetManagementContent = findViewById(R.id.tvVetManagementContent);
        tvPreventionSourcesContent = findViewById(R.id.tvPreventionSourcesContent);

        btnDetailRecordIllness = findViewById(R.id.btnDetailRecordIllness);
        btnDetailRecordTreatment = findViewById(R.id.btnDetailRecordTreatment);
        btnDetailViewHistory = findViewById(R.id.btnDetailViewHistory);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(guide != null ? guide.name : "Disease Guide");
        }

        if (guide == null) {
            // Explicit error handling: Never silently substitute another disease
            tvDetailTitle.setText("Disease Record Not Found");
            tvDetailAliases.setText("Unknown / Unrecognized Disease ID: " + (diseaseId != null ? diseaseId : "None"));
            tvDetailUrgencyBadge.setVisibility(View.GONE);
            tvDetailZoonoticBadge.setVisibility(View.GONE);
            tvDetailNotifiableBadge.setVisibility(View.GONE);

            tvOverviewContent.setText("The requested disease guide could not be found. Please return to the Veterinary Guide and select a disease.");
            tvSignsContent.setText("No clinical signs data available.");
            tvFarmerActionsContent.setText("No action data available.");
            tvVetManagementContent.setText("No veterinary management data available.");
            tvPreventionSourcesContent.setText("No prevention or source data available.");

            btnDetailRecordIllness.setEnabled(false);
            btnDetailRecordTreatment.setEnabled(false);
            btnDetailViewHistory.setEnabled(false);
            return;
        }

        populateGuideView();

        btnDetailRecordIllness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (guide == null) return;
                Intent intent = new Intent(DiseaseDetailActivity.this, IllnessActivity.class);
                String cleanName = guide.name.replaceAll("[^a-zA-Z0-9 ]", "").trim();
                intent.putExtra("type", cleanName);
                startActivity(intent);
            }
        });

        btnDetailRecordTreatment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (guide == null) return;
                Intent intent = new Intent(DiseaseDetailActivity.this, TreatmentActivity.class);
                intent.putExtra("disease_name", guide.name);
                startActivity(intent);
            }
        });

        btnDetailViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DiseaseDetailActivity.this, "Select an animal to view its full history", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DiseaseDetailActivity.this, MainActivity.class);
                intent.putExtra("navigate_to", "animal_list");
                startActivity(intent);
            }
        });
    }

    private void populateGuideView() {
        tvDetailTitle.setText(guide.name);
        tvDetailAliases.setText("Aliases / Local Names: " + (guide.aliases != null ? guide.aliases : "None"));

        tvDetailUrgencyBadge.setText(guide.urgency + " PRIORITY");
        if ("CRITICAL".equalsIgnoreCase(guide.urgency)) {
            tvDetailUrgencyBadge.setBackgroundColor(Color.parseColor("#B71C1C"));
        } else if ("HIGH".equalsIgnoreCase(guide.urgency)) {
            tvDetailUrgencyBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
        } else {
            tvDetailUrgencyBadge.setBackgroundColor(Color.parseColor("#F57C00"));
        }

        if (guide.zoonotic) {
            tvDetailZoonoticBadge.setVisibility(View.VISIBLE);
        } else {
            tvDetailZoonoticBadge.setVisibility(View.GONE);
        }

        if (guide.notifiable) {
            tvDetailNotifiableBadge.setVisibility(View.VISIBLE);
        } else {
            tvDetailNotifiableBadge.setVisibility(View.GONE);
        }

        // Overview
        StringBuilder sbOverview = new StringBuilder();
        sbOverview.append("• Species Affected: ").append(guide.species).append("\n");
        sbOverview.append("• Category: ").append(guide.category).append("\n");
        sbOverview.append("• Cause: ").append(guide.cause).append("\n");
        sbOverview.append("• Transmission: ").append(guide.transmission).append("\n");
        sbOverview.append("• Risk Factors: ").append(guide.riskFactors).append("\n");
        sbOverview.append("• Incubation Period: ").append(guide.incubation);
        tvOverviewContent.setText(sbOverview.toString());

        // Signs
        StringBuilder sbSigns = new StringBuilder();
        sbSigns.append("🔹 Early Warning Signs:\n");
        for (String sign : guide.earlySigns) {
            sbSigns.append("   - ").append(sign).append("\n");
        }
        sbSigns.append("\n🔸 Common Clinical Signs:\n");
        for (String sign : guide.clinicalSigns) {
            sbSigns.append("   - ").append(sign).append("\n");
        }
        sbSigns.append("\n🚨 Emergency / Severe Signs:\n");
        for (String sign : guide.emergencySigns) {
            sbSigns.append("   - ").append(sign).append("\n");
        }
        tvSignsContent.setText(sbSigns.toString().trim());

        // Farmer Checks & First Aid
        StringBuilder sbFarmer = new StringBuilder();
        sbFarmer.append("🔍 What the Farmer Can Check:\n");
        for (String check : guide.farmerChecks) {
            sbFarmer.append("   • ").append(check).append("\n");
        }
        sbFarmer.append("\n🛠️ Immediate First-Aid & Supportive Actions:\n");
        for (String action : guide.immediateActions) {
            sbFarmer.append("   • ").append(action).append("\n");
        }
        tvFarmerActionsContent.setText(sbFarmer.toString().trim());

        // Vet Management & Warnings
        StringBuilder sbVet = new StringBuilder();
        sbVet.append("🩺 Veterinary Management:\n").append(guide.veterinaryManagement).append("\n\n");
        sbVet.append("🔬 Diagnostic Methods:\n").append(guide.diagnostics).append("\n\n");
        sbVet.append("⚠️ Treatment Warnings:\n").append(guide.treatmentWarnings).append("\n\n");
        sbVet.append("🥛 Meat/Milk Withdrawal Warning:\n").append(guide.withdrawalWarning);
        tvVetManagementContent.setText(sbVet.toString());

        // Prevention & Sources
        tvPreventionSourcesContent.setText("🛡️ Prevention & Management:\n" + guide.prevention + "\n\n"
                + "💉 Vaccination Guidance:\n" + guide.vaccination + "\n\n"
                + "🔒 Biosecurity & Isolation:\n" + guide.biosecurity + "\n\n"
                + "📚 References / Authoritative Sources:\n" + guide.sources + "\n\n"
                + "🗓️ Last Reviewed: " + guide.lastReviewed);
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
