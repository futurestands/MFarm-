package dev.mfarm.com.mfarm;

import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
    private List<Map<String, String>> breedingRows = new ArrayList<>();
    private List<Map<String, String>> kiddingRows = new ArrayList<>();
    private List<Map<String, String>> vaccineRows = new ArrayList<>();
    private List<Map<String, String>> disposalRows = new ArrayList<>();
    private List<Map<String, String>> financialRows = new ArrayList<>();
    private List<Map<String, String>> cropRows = new ArrayList<>();
    private List<Map<String, String>> batchRows = new ArrayList<>();
    private boolean isLoaded = false;
    private Uri importUri;
    private String fileName = "Unknown File";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_preview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String uriStr = getIntent().getStringExtra("import_uri");
        if (uriStr != null) {
            importUri = Uri.parse(uriStr);
            fileName = getFileName(importUri);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Import Preview: " + fileName);
        }

        btnLoadWorkbook = findViewById(R.id.btnLoadWorkbook);
        btnApproveImport = findViewById(R.id.btnApproveImport);
        btnResolveAmbiguous = findViewById(R.id.btnResolveAmbiguous);
        tvImportStats = findViewById(R.id.tvImportStats);
        lvPreviewDetails = findViewById(R.id.lvPreviewDetails);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, previewLogs);
        lvPreviewDetails.setAdapter(adapter);

        btnLoadWorkbook.setText("Parse Selected File");
        btnLoadWorkbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (importUri != null) {
                    parseCsvFile(importUri);
                } else {
                    Toast.makeText(ImportPreviewActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                }
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

    private String getFileName(Uri uri) {
        String result = "Farm_Template_File.csv";
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        result = cursor.getString(idx);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        } else if (uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void parseCsvFile(Uri uri) {
        flockRows.clear();
        breedingRows.clear();
        kiddingRows.clear();
        vaccineRows.clear();
        disposalRows.clear();
        financialRows.clear();
        cropRows.clear();
        batchRows.clear();
        previewLogs.clear();

        int parsedRows = 0;
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            String headerLine = null;
            List<String> headers = null;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                if (headerLine == null) {
                    headerLine = line.toLowerCase();
                    headers = Arrays.asList(headerLine.split(","));
                    continue;
                }

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                Map<String, String> rowMap = new HashMap<>();
                for (int i = 0; i < headers.size() && i < parts.length; i++) {
                    String val = parts[i].replace("\"", "").trim();
                    if (val.startsWith("'")) val = val.substring(1);
                    rowMap.put(headers.get(i).trim(), val);
                }

                parsedRows++;

                // Route based on detected headers
                if (headers.contains("tag_no") && headers.contains("species") && headers.contains("dob")) {
                    flockRows.add(rowMap);
                } else if (headers.contains("batch_id") && headers.contains("initial_quantity")) {
                    batchRows.add(rowMap);
                } else if (headers.contains("female_tag") && headers.contains("mating_date")) {
                    breedingRows.add(rowMap);
                } else if (headers.contains("disease_name") && headers.contains("vaccine_name")) {
                    vaccineRows.add(rowMap);
                } else if (headers.contains("disposal_date") && headers.contains("method")) {
                    disposalRows.add(rowMap);
                } else if (headers.contains("crop_type") && headers.contains("activity_type")) {
                    cropRows.add(rowMap);
                } else if (headers.contains("type") && headers.contains("amount") && headers.contains("project_code")) {
                    financialRows.add(rowMap);
                } else if (headers.contains("eggs_collected") && headers.contains("batch_id")) {
                    // Eggs route - handled in batches or separate? Ignore for now or route appropriately.
                }
            }
            reader.close();

            previewLogs.add("=== WORKBOOK PARSED: " + fileName + " ===");
            if (!flockRows.isEmpty()) previewLogs.add("• INDIVIDUAL LIVESTOCK — " + flockRows.size() + " records");
            if (!batchRows.isEmpty()) previewLogs.add("• LIVESTOCK BATCHES — " + batchRows.size() + " records");
            if (!breedingRows.isEmpty()) previewLogs.add("• BREEDING & BIRTHS — " + breedingRows.size() + " records");
            if (!vaccineRows.isEmpty()) previewLogs.add("• HEALTH & VACCINES — " + vaccineRows.size() + " records");
            if (!disposalRows.isEmpty()) previewLogs.add("• DISPOSALS — " + disposalRows.size() + " records");
            if (!cropRows.isEmpty()) previewLogs.add("• CROP ACTIVITIES — " + cropRows.size() + " records");
            if (!financialRows.isEmpty()) previewLogs.add("• FINANCES — " + financialRows.size() + " records");

            if (parsedRows == 0) {
                previewLogs.add("No valid records found. Ensure you are using a standard MFarm CSV Template.");
            }

            tvImportStats.setText(String.format(Locale.US, "Parsed %d total records from CSV\nStatus: Ready for Review & Approval", parsedRows));
            adapter.notifyDataSetChanged();
            isLoaded = parsedRows > 0;
            btnApproveImport.setEnabled(isLoaded);
            btnResolveAmbiguous.setEnabled(!flockRows.isEmpty());

        } catch (Exception e) {
            Toast.makeText(this, "Error parsing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void resolveAmbiguousTagsDialog() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter Permanent Tag for 'New Born' (e.g. GT-2026-05)");

        new AlertDialog.Builder(this)
                .setTitle("Resolve Ambiguous Newborn Tag")
                .setMessage("Records use temporary tag 'New Born'. Assign a unique permanent tag number for import:")
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
        animCount += ExcelImporter.importLivestockBatches(adapterDb, batchRows, summary);
        int breedCount = ExcelImporter.importBreedingRecords(adapterDb, breedingRows, summary);
        int kidCount = ExcelImporter.importKiddingRecords(adapterDb, kiddingRows, summary);
        int vacCount = ExcelImporter.importVaccinationSchedules(adapterDb, vaccineRows, summary);
        int dispCount = ExcelImporter.importDisposalRecords(adapterDb, disposalRows, summary);
        int finCount = ExcelImporter.importFinancialRecords(adapterDb, financialRows, summary);
        int cropCount = ExcelImporter.importCropActivities(adapterDb, cropRows, summary);

        ExcelImporter.recordImportBatch(adapterDb, summary);

        new AlertDialog.Builder(this)
                .setTitle("Import Complete")
                .setMessage(String.format(Locale.US,
                        "Successfully Imported Batch:\n• %d Livestock/Batches\n• %d Breeding Records\n• %d Kidding Records\n• %d Vaccinations\n• %d Disposal Logs\n• %d Financial Entries\n• %d Crop Activities\n• %d Duplicate Records Skipped",
                        animCount, breedCount, kidCount, vacCount, dispCount, finCount, cropCount, summary.duplicateSkipped))
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

