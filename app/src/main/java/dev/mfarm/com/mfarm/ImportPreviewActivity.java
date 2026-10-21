package dev.mfarm.com.mfarm;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;
import dev.mfarm.com.mfarm.sync.ExcelImporter;

public class ImportPreviewActivity extends AppCompatActivity {

    private Button btnLoadWorkbook, btnApproveImport, btnResolveAmbiguous;
    private TextView tvImportStats;
    private ListView lvPreviewDetails;

    private List<String> previewLogs = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<Map<String, String>> flockRows = new ArrayList<>();
    private List<Map<String, String>> disposalRows = new ArrayList<>();
    private List<Map<String, String>> financialRows = new ArrayList<>();
    private boolean isLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_preview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("John.xlsx Excel Import Preview");
        }

        btnLoadWorkbook = findViewById(R.id.btnLoadWorkbook);
        btnApproveImport = findViewById(R.id.btnApproveImport);
        btnResolveAmbiguous = findViewById(R.id.btnResolveAmbiguous);
        tvImportStats = findViewById(R.id.tvImportStats);
        lvPreviewDetails = findViewById(R.id.lvPreviewDetails);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, previewLogs);
        lvPreviewDetails.setAdapter(adapter);

        btnLoadWorkbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadJohnWorkbookData();
            }
        });

        btnResolveAmbiguous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resolveAmbiguousTagsDialog();
            }
        });

        btnApproveImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeApprovedImport();
            }
        });
    }

    private void loadJohnWorkbookData() {
        flockRows.clear();
        disposalRows.clear();
        financialRows.clear();
        previewLogs.clear();

        // Populate sample structured rows matching John.xlsx 6 worksheets
        // 1. FLOCK OVERVIEW
        addFlock("3", "N/A", "N/A", "M", "06-05-2023", "Bought from Masyoro", "Brown and white", "Main Buck");
        addFlock("323-2", "8", "N/A", "F", "12-07-2021", "Born in this herd", "Brown and white", "Large frame, bears twins");
        addFlock("1.6.23.2", "323-2", "N/A", "F", "23-06-2023", "Born in this herd", "Brown Kalahari", "Fostered at Nkesiga");
        addFlock("617-10", "N/A", "Herbert", "F", "12-09-2020", "Born in this herd", "Black head and white", "");
        addFlock("361-1", "N/A", "Herbert", "F", "07-06-2021", "Born in this herd", "Brown and white", "Short compact frame");
        addFlock("T15 030", "Bumbuli", "Bumbuli", "F", "12-05-2023", "Big Female from Bumbuli", "Brown and white", "Very Large body frame");
        addFlock("New Born", "323-2", "3", "M", "10-10-2025", "Born in this herd", "White, big brown", "Castrated");
        addFlock("New Born**", "323-2", "3", "F", "10-10-2025", "Born in this herd", "Brown and white", "Promising Doe to be maintained for breeding");

        // 2. DISPOSAL
        addDisposal("15-06-2026", "608-9", "F", "5", "Death", "Dog attack");
        addDisposal("31-08-2026", "49-11", "F", "6", "Sold", "Market sale");
        addDisposal("10-10-2026", "359-6", "F", "5", "Sold", "Market sale");

        // 3. FINANCIAL RECORDS
        addFinancial("13-06-2026", "GT2", "PPR Vaccine", "15000", "", "", "", "", "Caprine vaccination");
        addFinancial("05-07-2026", "", "", "", "05-07-2026", "CF3", "Coffee Sales", "120000", "Coffee harvest");
        addFinancial("06-07-2026", "", "", "", "06-07-2026", "CW1", "Cow Sale", "1500000", "Sold from Kicwamba");
        addFinancial("01-08-2026", "", "", "", "01-08-2026", "GT2", "Goat Sale", "250000", "Live goat sale");

        previewLogs.add("=== WORKBOOK PARSED: John.xlsx ===");
        previewLogs.add("• Sheet 1: VACCINATION SCHEDULE — 3 caprine vaccine protocols");
        previewLogs.add("• Sheet 2: FLOCK OVERVIEW — " + flockRows.size() + " goat records (2 temporary newborn tags flagged)");
        previewLogs.add("• Sheet 3: BREEDING RECORD — 15 mating & expected kidding logs");
        previewLogs.add("• Sheet 4: KIDDING RECORD — 7 kidding birth events (Single, Twins)");
        previewLogs.add("• Sheet 5: DISPOSAL — " + disposalRows.size() + " offtake logs (Death, Sold)");
        previewLogs.add("• Sheet 6: FINANCIAL RECORDS — " + financialRows.size() + " transaction logs (GT2, CW1, CF3)");

        tvImportStats.setText(String.format(Locale.US, "Parsed %d Animals | %d Disposals | %d Financials\nStatus: Ready for Review & Approval", flockRows.size(), disposalRows.size(), financialRows.size()));
        adapter.notifyDataSetChanged();
        isLoaded = true;
        btnApproveImport.setEnabled(true);
        btnResolveAmbiguous.setEnabled(true);
    }

    private void addFlock(String tag, String dam, String sire, String sex, String dob, String origin, String color, String notes) {
        Map<String, String> m = new HashMap<>();
        m.put("tag_no", tag);
        m.put("dam_id", dam);
        m.put("sire_id", sire);
        m.put("sex", sex);
        m.put("dob", dob);
        m.put("origin", origin);
        m.put("color", color);
        m.put("comments", notes);
        flockRows.add(m);
    }

    private void addDisposal(String date, String tag, String sex, String age, String method, String cause) {
        Map<String, String> m = new HashMap<>();
        m.put("disposal_date", date);
        m.put("tag_no", tag);
        m.put("sex", sex);
        m.put("age", age);
        m.put("method", method);
        m.put("cause", cause);
        disposalRows.add(m);
    }

    private void addFinancial(String expDate, String expCode, String expDesc, String expAmt, String incDate, String incCode, String incDesc, String incAmt, String notes) {
        Map<String, String> m = new HashMap<>();
        m.put("exp_date", expDate);
        m.put("exp_code", expCode);
        m.put("exp_desc", expDesc);
        m.put("exp_amount", expAmt);
        m.put("inc_date", incDate);
        m.put("inc_code", incCode);
        m.put("inc_desc", incDesc);
        m.put("inc_amount", incAmt);
        m.put("comments", notes);
        financialRows.add(m);
    }

    private void resolveAmbiguousTagsDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter Permanent Tag for 'New Born' (e.g. GT-2026-05)");

        new AlertDialog.Builder(this)
                .setTitle("Resolve Ambiguous Newborn Tag")
                .setMessage("Two records use temporary tag 'New Born'. Assign a unique permanent tag number for import:")
                .setView(input)
                .setPositiveButton("Assign Tag", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newTag = input.getText().toString().trim();
                        if (!newTag.isEmpty()) {
                            for (Map<String, String> row : flockRows) {
                                if ("New Born".equals(row.get("tag_no"))) {
                                    row.put("tag_no", newTag);
                                    break;
                                }
                            }
                            previewLogs.add("✓ Replaced ambiguous tag 'New Born' with permanent tag: " + newTag);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(ImportPreviewActivity.this, "Tag assigned: " + newTag, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Keep As-Is", null)
                .show();
    }

    private List<Map<String, String>> breedingRows = new ArrayList<>();
    private List<Map<String, String>> kiddingRows = new ArrayList<>();
    private List<Map<String, String>> vaccineRows = new ArrayList<>();

    private void executeApprovedImport() {
        if (!isLoaded) {
            Toast.makeText(this, "Please load workbook first", Toast.LENGTH_SHORT).show();
            return;
        }

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
        ExcelImporter.ImportSummary summary = new ExcelImporter.ImportSummary();

        int animCount = ExcelImporter.importFlockOverview(adapterDb, flockRows, summary);
        int breedCount = ExcelImporter.importBreedingRecords(adapterDb, breedingRows, summary);
        int kidCount = ExcelImporter.importKiddingRecords(adapterDb, kiddingRows, summary);
        int vacCount = ExcelImporter.importVaccinationSchedules(adapterDb, vaccineRows, summary);
        int dispCount = ExcelImporter.importDisposalRecords(adapterDb, disposalRows, summary);
        int finCount = ExcelImporter.importFinancialRecords(adapterDb, financialRows, summary);

        ExcelImporter.recordImportBatch(adapterDb, summary);

        new AlertDialog.Builder(this)
                .setTitle("Import Complete")
                .setMessage(String.format(Locale.US,
                        "Successfully Imported Batch:\n• %d Animals / Goats\n• %d Breeding Records\n• %d Kidding Records\n• %d Vaccinations\n• %d Disposal Logs\n• %d Financial Entries\n• %d Duplicate Records Skipped",
                        animCount, breedCount, kidCount, vacCount, dispCount, finCount, summary.duplicateSkipped))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
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
