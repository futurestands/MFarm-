package dev.mfarm.com.mfarm;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private TextInputEditText etSearchQuery;
    private ListView lvSearchResults;
    private List<String> searchResults = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Records");
        }

        etSearchQuery = findViewById(R.id.etSearchQuery);
        lvSearchResults = findViewById(R.id.lvSearchResults);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, searchResults);
        lvSearchResults.setAdapter(adapter);

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        performSearch("");
    }

    private void performSearch(String query) {
        searchResults.clear();
        String wild = "%" + query + "%";

        // Search Animals
        Cursor c1 = MainActivity.database.rawQuery("SELECT name, gender, dob, lifecycle_status, repro_status FROM animas WHERE name LIKE ? OR gender LIKE ? OR lifecycle_status LIKE ?", new String[]{wild, wild, wild});
        if (c1.moveToFirst()) {
            do {
                searchResults.add("[ANIMAL] " + c1.getString(0) + " (" + c1.getString(1) + ", DOB: " + c1.getString(2) + ")\nStatus: " + c1.getString(3) + " | " + c1.getString(4));
            } while (c1.moveToNext());
        }
        c1.close();

        // Search Illness & Health
        Cursor c2 = MainActivity.database.rawQuery("SELECT animal_name, illness_occured, date_occured, diagnosis FROM illness WHERE animal_name LIKE ? OR illness_occured LIKE ? OR diagnosis LIKE ?", new String[]{wild, wild, wild});
        if (c2.moveToFirst()) {
            do {
                searchResults.add("[HEALTH] " + c2.getString(0) + " - " + c2.getString(1) + " (" + c2.getString(2) + ")\nDiagnosis: " + c2.getString(3));
            } while (c2.moveToNext());
        }
        c2.close();

        // Search Inventory
        Cursor c3 = MainActivity.database.rawQuery("SELECT item_name, category, quantity, unit FROM inventory WHERE item_name LIKE ? OR category LIKE ?", new String[]{wild, wild});
        if (c3.moveToFirst()) {
            do {
                searchResults.add("[INVENTORY] " + c3.getString(0) + " [" + c3.getString(1) + "]: " + c3.getDouble(2) + " " + c3.getString(3));
            } while (c3.moveToNext());
        }
        c3.close();

        // Search Expenses & Income
        String currencySymbol = "UGX ";
        try {
            Cursor cur = MainActivity.database.rawQuery("SELECT currency_symbol FROM farm_profile LIMIT 1", null);
            if (cur.moveToFirst()) {
                String sym = cur.getString(0);
                if (sym != null && !sym.isEmpty() && !"$".equals(sym)) {
                    currencySymbol = sym + (sym.endsWith(" ") ? "" : " ");
                }
            }
            cur.close();
        } catch (Exception ignored) {}

        Cursor c4 = MainActivity.database.rawQuery("SELECT category, amount, date, description FROM expenses WHERE category LIKE ? OR description LIKE ?", new String[]{wild, wild});
        if (c4.moveToFirst()) {
            do {
                searchResults.add("[EXPENSE] " + c4.getString(0) + ": " + currencySymbol + String.format(java.util.Locale.US, "%,.0f", c4.getDouble(1)) + " (" + c4.getString(2) + " - " + c4.getString(3) + ")");
            } while (c4.moveToNext());
        }
        c4.close();

        Cursor c5 = MainActivity.database.rawQuery("SELECT category, amount, date, description FROM income WHERE category LIKE ? OR description LIKE ?", new String[]{wild, wild});
        if (c5.moveToFirst()) {
            do {
                searchResults.add("[INCOME] " + c5.getString(0) + ": " + currencySymbol + String.format(java.util.Locale.US, "%,.0f", c5.getDouble(1)) + " (" + c5.getString(2) + " - " + c5.getString(3) + ")");
            } while (c5.moveToNext());
        }
        c5.close();

        if (searchResults.isEmpty()) {
            searchResults.add("No matching records found.");
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
