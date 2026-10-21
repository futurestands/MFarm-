package dev.mfarm.com.mfarm;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class ImportHistoryActivity extends AppCompatActivity {

    private ListView lvImportHistory;
    private List<String> historyEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Workbook Import History");
        }

        lvImportHistory = findViewById(R.id.lvImportHistory);
        loadHistory();
    }

    private void loadHistory() {
        historyEntries.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT file_name, imported_at, record_count, summary_json FROM import_batches ORDER BY id DESC LIMIT 100", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String fileName = cursor.getString(0);
                long ts = cursor.getLong(1);
                int count = cursor.getInt(2);
                String json = cursor.getString(3);
                String dateStr = sdf.format(new Date(ts));

                historyEntries.add(dateStr + " — " + fileName + "\nRecords Imported: " + count + "\nDetails: " + (json != null ? json : ""));
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        if (historyEntries.isEmpty()) {
            historyEntries.add("No workbook imports performed yet.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyEntries);
        lvImportHistory.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
