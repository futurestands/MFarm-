package dev.mfarm.com.mfarm;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;
import dev.mfarm.com.mfarm.sync.ExcelExporter;
import dev.mfarm.com.mfarm.sync.ExcelImporter;
import dev.mfarm.com.mfarm.sync.TemplateManager;

public class ExcelManagementActivity extends AppCompatActivity {

    private Button btnImportExcel, btnDownloadTemplates, btnExportRecords;
    private Spinner spinnerExportModule, spinnerExportEnterprise;
    private ListView lvImportBatches;

    private List<String> batchDisplayList = new ArrayList<>();
    private List<String> batchUuids = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Excel Management Centre");
        }

        btnImportExcel = findViewById(R.id.btnImportExcel);
        btnDownloadTemplates = findViewById(R.id.btnDownloadTemplates);
        btnExportRecords = findViewById(R.id.btnExportRecords);
        spinnerExportModule = findViewById(R.id.spinnerExportModule);
        spinnerExportEnterprise = findViewById(R.id.spinnerExportEnterprise);
        lvImportBatches = findViewById(R.id.lvImportBatches);

        setupExportSpinners();

        btnImportExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimetypes = {"text/csv", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                startActivityForResult(intent, 1001);
            }
        });

        btnDownloadTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadOfficialTemplates();
            }
        });

        btnExportRecords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeExport();
            }
        });

        lvImportBatches.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < batchUuids.size()) {
                showReversalDialog(batchUuids.get(position));
            }
        });

        loadImportBatches();
    }

    private void setupExportSpinners() {
        String[] modules = {"All Farm Data", "Animals", "Batches", "Disposals", "Crops", "Finances"};
        ArrayAdapter<String> modAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modules);
        modAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExportModule.setAdapter(modAdapter);

        String[] enterprises = {"ALL", "GT2 (Goats)", "CW1 (Cattle)", "CF3 (Coffee)", "PL4 (Poultry)", "RB5 (Rabbits)", "GEN (General)"};
        ArrayAdapter<String> entAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, enterprises);
        entAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExportEnterprise.setAdapter(entAdapter);
    }

    private void downloadOfficialTemplates() {
        Map<String, File> templates = TemplateManager.generateAllTemplates(this);
        File dir = TemplateManager.getTemplateDirectory(this);

        new AlertDialog.Builder(this)
                .setTitle("Official Templates Generated")
                .setMessage("Generated " + templates.size() + " official CSV/Excel templates in downloads directory:\n" + dir.getAbsolutePath() + "\n\nIncludes instructions, required fields, and sample rows.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void executeExport() {
        String module = spinnerExportModule.getSelectedItem().toString();
        String enterpriseStr = spinnerExportEnterprise.getSelectedItem().toString();
        String codeFilter = enterpriseStr.split(" ")[0];

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
        File exportDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "MFarm_Exports");
        File exportedFile = ExcelExporter.exportModuleToCsv(adapterDb, exportDir, module, codeFilter);

        if (exportedFile != null && exportedFile.exists()) {
            new AlertDialog.Builder(this)
                    .setTitle("Export Complete")
                    .setMessage("Exported records successfully to:\n" + exportedFile.getAbsolutePath() + "\n\nFile Size: " + (exportedFile.length() / 1024) + " KB")
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImportBatches() {
        batchDisplayList.clear();
        batchUuids.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT batch_uuid, file_name, imported_at, record_count, status, summary_json FROM import_batches ORDER BY id DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String uuid = cursor.getString(0);
                String fileName = cursor.getString(1);
                long ts = cursor.getLong(2);
                int count = cursor.getInt(3);
                String status = cursor.getString(4);
                String json = cursor.getString(5);

                batchUuids.add(uuid);
                batchDisplayList.add(sdf.format(new Date(ts)) + " — " + fileName + " [" + status + "]\nRecords: " + count + " | UUID: " + uuid.substring(0, 8) + "...\n" + (json != null ? json : ""));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        if (batchDisplayList.isEmpty()) {
            batchDisplayList.add("No import batches recorded yet.");
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, batchDisplayList);
        lvImportBatches.setAdapter(adapter);
    }

    private void showReversalDialog(final String batchUuid) {
        new AlertDialog.Builder(this)
                .setTitle("Reverse / Undo Import Batch")
                .setMessage("Are you sure you want to reverse batch " + batchUuid.substring(0, 8) + "?\n\nThis will safely remove inserted records and restore modified values without deleting unrelated farm data.")
                .setPositiveButton("Reverse Import", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeBatchReversal(batchUuid);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeBatchReversal(String batchUuid) {
        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);
        boolean success = ExcelImporter.reverseImportBatch(adapterDb, batchUuid);

        if (success) {
            Toast.makeText(this, "Import batch reversed successfully!", Toast.LENGTH_LONG).show();
            loadImportBatches();
        } else {
            Toast.makeText(this, "Could not reverse import batch (conflict or already reversed)", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            Intent intent = new Intent(this, ImportPreviewActivity.class);
            intent.putExtra("import_uri", uri.toString());
            startActivity(intent);
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
