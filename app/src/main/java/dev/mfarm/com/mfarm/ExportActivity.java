package dev.mfarm.com.mfarm;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class ExportActivity extends AppCompatActivity {

    private String farmName = "MFarm";
    private String currencySymbol = "$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reports & Data Export");
        }

        loadFarmProfile();

        findViewById(R.id.btnExportAnimals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportAnimalReport();
            }
        });

        findViewById(R.id.btnExportMilk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportMilkReport();
            }
        });

        findViewById(R.id.btnExportHealth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportHealthReport();
            }
        });

        findViewById(R.id.btnExportBreeding).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportBreedingReport();
            }
        });

        findViewById(R.id.btnExportExpenses).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportFinancialReport();
            }
        });
    }

    private void loadFarmProfile() {
        Cursor cursor = MainActivity.database.rawQuery("SELECT farm_name, currency_symbol FROM farm_profile LIMIT 1", null);
        if (cursor.moveToFirst()) {
            if (cursor.getString(0) != null && !cursor.getString(0).isEmpty()) {
                farmName = cursor.getString(0);
            }
            if (cursor.getString(1) != null && !cursor.getString(1).isEmpty()) {
                currencySymbol = cursor.getString(1);
            }
        }
        cursor.close();
    }

    private void exportAnimalReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(farmName).append(" — Animal Inventory Report ===\n\n");

        int total = 0, male = 0, female = 0, active = 0, pregnant = 0, lactating = 0, sick = 0, sold = 0;
        Cursor c = MainActivity.database.rawQuery("SELECT gender, lifecycle_status, repro_status, lactation_status, health_status FROM animas", null);
        if (c.moveToFirst()) {
            do {
                total++;
                if ("Male".equalsIgnoreCase(c.getString(0))) male++;
                if ("Female".equalsIgnoreCase(c.getString(0))) female++;
                if ("Active".equalsIgnoreCase(c.getString(1))) active++;
                if ("Sold".equalsIgnoreCase(c.getString(1))) sold++;
                if ("Pregnant".equalsIgnoreCase(c.getString(2))) pregnant++;
                if ("Lactating".equalsIgnoreCase(c.getString(3))) lactating++;
                if ("Sick".equalsIgnoreCase(c.getString(4))) sick++;
            } while (c.moveToNext());
        }
        c.close();

        sb.append(String.format(Locale.US, "Total Herd Size: %d\n- Active: %d | Sold: %d\n- Female: %d | Male: %d\n- Pregnant: %d | Lactating: %d | Sick: %d\n\n",
                total, active, sold, female, male, pregnant, lactating, sick));

        sb.append("--- Detailed Animal Records ---\n");
        Cursor c2 = MainActivity.database.rawQuery("SELECT a.name, a.gender, a.dob, b.name FROM animas a LEFT JOIN breeds b ON a.breed_id = b.id", null);
        if (c2.moveToFirst()) {
            do {
                sb.append("• ").append(c2.getString(0)).append(" | Breed: ").append(c2.getString(3))
                        .append(" | Gender: ").append(c2.getString(1)).append(" | DOB: ").append(c2.getString(2)).append("\n");
            } while (c2.moveToNext());
        }
        c2.close();

        shareReport("Animal Inventory Report", sb.toString());
    }

    private void exportMilkReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(farmName).append(" — Milk Yield Report ===\n\n");

        double totalLitres = 0;
        Cursor c1 = MainActivity.database.rawQuery("SELECT SUM(litres) FROM milk_production", null);
        if (c1.moveToFirst()) {
            totalLitres = c1.getDouble(0);
        }
        c1.close();

        sb.append(String.format(Locale.US, "Cumulative Milk Production: %.2f Litres\n\n", totalLitres));
        sb.append("--- Recent Milk Yield Logs ---\n");

        Cursor c2 = MainActivity.database.rawQuery("SELECT m.datetime, a.name, m.litres, m.description FROM milk_production m JOIN animas a ON m.animal_id = a.id ORDER BY m.id DESC LIMIT 50", null);
        if (c2.moveToFirst()) {
            do {
                sb.append("• ").append(c2.getString(0)).append(" | Cow: ").append(c2.getString(1))
                        .append(" | Yield: ").append(c2.getDouble(2)).append(" L");
                if (c2.getString(3) != null && !c2.getString(3).isEmpty()) {
                    sb.append(" (").append(c2.getString(3)).append(")");
                }
                sb.append("\n");
            } while (c2.moveToNext());
        }
        c2.close();

        shareReport("Milk Production Report", sb.toString());
    }

    private void exportHealthReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(farmName).append(" — Health & Vaccination Report ===\n\n");

        sb.append("--- Scheduled Vaccinations ---\n");
        Cursor c1 = MainActivity.database.rawQuery("SELECT a.name, v.vaccine_name, v.scheduled_date, v.status FROM vaccinations v JOIN animas a ON v.animal_id = a.id ORDER BY v.id DESC", null);
        if (c1.moveToFirst()) {
            do {
                sb.append("• ").append(c1.getString(0)).append(" | Vaccine: ").append(c1.getString(1))
                        .append(" | Date: ").append(c1.getString(2)).append(" | Status: ").append(c1.getString(3)).append("\n");
            } while (c1.moveToNext());
        } else {
            sb.append("No vaccination records found.\n");
        }
        c1.close();

        sb.append("\n--- Illness & Treatments ---\n");
        Cursor c2 = MainActivity.database.rawQuery("SELECT animal_name, illness_occured, date_occured, diagnosis, treatment, cost FROM illness ORDER BY id DESC", null);
        if (c2.moveToFirst()) {
            do {
                sb.append("• ").append(c2.getString(0)).append(" | Event: ").append(c2.getString(1))
                        .append(" | Date: ").append(c2.getString(2)).append("\n  Diagnosis: ").append(c2.getString(3))
                        .append(" | Treatment: ").append(c2.getString(4)).append(" | Cost: ").append(currencySymbol).append(c2.getDouble(5)).append("\n");
            } while (c2.moveToNext());
        } else {
            sb.append("No illness/treatment records found.\n");
        }
        c2.close();

        shareReport("Health & Vaccination Report", sb.toString());
    }

    private void exportBreedingReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(farmName).append(" — Breeding & Calving Report ===\n\n");

        sb.append("--- Breeding Records ---\n");
        Cursor c1 = MainActivity.database.rawQuery("SELECT a.name, b.mating_date, b.expected_birth_date, b.status FROM breeding_records b JOIN animas a ON b.animal_id = a.id ORDER BY b.id DESC", null);
        if (c1.moveToFirst()) {
            do {
                sb.append("• Cow: ").append(c1.getString(0)).append(" | Mated: ").append(c1.getString(1))
                        .append(" | Expected Calving: ").append(c1.getString(2)).append(" | Status: ").append(c1.getString(3)).append("\n");
            } while (c1.moveToNext());
        } else {
            sb.append("No breeding records found.\n");
        }
        c1.close();

        sb.append("\n--- Calving Records ---\n");
        Cursor c2 = MainActivity.database.rawQuery("SELECT c.birth_date, d.name, c.sex, c.birth_weight, c.survival_status FROM calving_records c JOIN animas d ON c.dam_id = d.id ORDER BY c.id DESC", null);
        if (c2.moveToFirst()) {
            do {
                sb.append("• Dam: ").append(c2.getString(1)).append(" | Calved: ").append(c2.getString(0))
                        .append(" | Sex: ").append(c2.getString(2)).append(" | Weight: ").append(c2.getDouble(3)).append("kg | Status: ").append(c2.getString(4)).append("\n");
            } while (c2.moveToNext());
        } else {
            sb.append("No calving records found.\n");
        }
        c2.close();

        shareReport("Breeding & Calving Report", sb.toString());
    }

    private void exportFinancialReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(farmName).append(" — Financial P&L Statement ===\n\n");

        double totalIncome = 0;
        double totalExpense = 0;

        Cursor c1 = MainActivity.database.rawQuery("SELECT SUM(amount) FROM income", null);
        if (c1.moveToFirst()) totalIncome = c1.getDouble(0);
        c1.close();

        Cursor c2 = MainActivity.database.rawQuery("SELECT SUM(amount) FROM expenses", null);
        if (c2.moveToFirst()) totalExpense = c2.getDouble(0);
        c2.close();

        sb.append(String.format(Locale.US, "Total Income: %s%.2f\nTotal Expenses: %s%.2f\nNet Profit / Loss: %s%.2f\n\n",
                currencySymbol, totalIncome, currencySymbol, totalExpense, currencySymbol, totalIncome - totalExpense));

        sb.append("--- Income Breakdown ---\n");
        Cursor c3 = MainActivity.database.rawQuery("SELECT date, category, amount, description FROM income ORDER BY id DESC LIMIT 25", null);
        if (c3.moveToFirst()) {
            do {
                sb.append("• [INCOME] ").append(c3.getString(0)).append(" | ").append(c3.getString(1))
                        .append(": ").append(currencySymbol).append(c3.getDouble(2)).append(" - ").append(c3.getString(3)).append("\n");
            } while (c3.moveToNext());
        }
        c3.close();

        sb.append("\n--- Expense Breakdown ---\n");
        Cursor c4 = MainActivity.database.rawQuery("SELECT date, category, amount, description FROM expenses ORDER BY id DESC LIMIT 25", null);
        if (c4.moveToFirst()) {
            do {
                sb.append("• [EXPENSE] ").append(c4.getString(0)).append(" | ").append(c4.getString(1))
                        .append(": ").append(currencySymbol).append(c4.getDouble(2)).append(" - ").append(c4.getString(3)).append("\n");
            } while (c4.moveToNext());
        }
        c4.close();

        shareReport("Financial P&L Statement", sb.toString());
    }

    private void shareReport(String title, String content) {
        DatabaseHelper.logAudit(MainActivity.database, "EXPORT_REPORT", "REPORTS", 0, "Exported: " + title);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, farmName + " — " + title);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(intent, "Share Report via"));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
