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

public class AuditLogActivity extends AppCompatActivity {

    private ListView lvAuditLogs;
    private List<String> logEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_log);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Audit Logs");
        }

        lvAuditLogs = findViewById(R.id.lvAuditLogs);
        loadLogs();
    }

    private void loadLogs() {
        logEntries.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Cursor cursor = MainActivity.database.rawQuery("SELECT timestamp, action, module, details FROM audit_logs ORDER BY id DESC LIMIT 100", null);
        if (cursor.moveToFirst()) {
            do {
                long ts = cursor.getLong(0);
                String action = cursor.getString(1);
                String module = cursor.getString(2);
                String details = cursor.getString(3);
                String dateStr = sdf.format(new Date(ts));

                logEntries.add(dateStr + " [" + module + "]\n" + action + ": " + (details != null ? details : ""));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (logEntries.isEmpty()) {
            logEntries.add("No audit log records found yet.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logEntries);
        lvAuditLogs.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
