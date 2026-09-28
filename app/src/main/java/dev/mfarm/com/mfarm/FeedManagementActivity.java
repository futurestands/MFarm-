package dev.mfarm.com.mfarm;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class FeedManagementActivity extends AppCompatActivity {

    private Spinner spinnerFeedItem;
    private TextInputEditText etConsumeQuantity, etTargetGroup, etFeedNotes;
    private Button btnRecordConsumption;
    private TextView tvFeedStockList;

    private List<String> feedNames = new ArrayList<>();
    private List<Integer> feedIds = new ArrayList<>();
    private List<Double> feedQuantities = new ArrayList<>();
    private List<String> feedUnits = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Feed Management");
        }

        spinnerFeedItem = findViewById(R.id.spinnerFeedItem);
        etConsumeQuantity = findViewById(R.id.etConsumeQuantity);
        etTargetGroup = findViewById(R.id.etTargetGroup);
        etFeedNotes = findViewById(R.id.etFeedNotes);
        btnRecordConsumption = findViewById(R.id.btnRecordConsumption);
        tvFeedStockList = findViewById(R.id.tvFeedStockList);

        loadFeeds();

        btnRecordConsumption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordConsumption();
            }
        });
    }

    private android.database.sqlite.SQLiteDatabase getDb() {
        return DatabaseHelper.getDatabase(this);
    }

    private void loadFeeds() {
        feedIds.clear();
        feedNames.clear();
        feedQuantities.clear();
        feedUnits.clear();

        StringBuilder stockSummary = new StringBuilder();

        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT id, item_name, quantity, unit, min_quantity FROM inventory WHERE category = 'Feed' OR category = 'Feeds' OR category LIKE '%feed%' ORDER BY item_name ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                double qty = cursor.getDouble(2);
                String unit = cursor.getString(3);
                double minQty = cursor.getDouble(4);

                feedIds.add(id);
                feedNames.add(name + " (" + qty + " " + unit + ")");
                feedQuantities.add(qty);
                feedUnits.add(unit);

                stockSummary.append("• ").append(name).append(": ").append(qty).append(" ").append(unit);
                if (qty <= minQty) {
                    stockSummary.append("  [LOW STOCK ALERT]");
                }
                stockSummary.append("\n");
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        if (feedNames.isEmpty()) {
            feedNames.add("No Feed Inventory Items Found");
            stockSummary.append("No feed items found in inventory. Add feed items in Inventory first.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, feedNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFeedItem.setAdapter(adapter);

        tvFeedStockList.setText(stockSummary.toString());
    }

    private void recordConsumption() {
        int selectedPos = spinnerFeedItem.getSelectedItemPosition();
        if (feedIds.isEmpty() || selectedPos < 0 || selectedPos >= feedIds.size()) {
            Toast.makeText(this, "Please select a feed item", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etConsumeQuantity.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Please enter quantity consumed", Toast.LENGTH_SHORT).show();
            return;
        }

        double consumeQty = Double.parseDouble(qtyStr);
        int feedId = feedIds.get(selectedPos);
        double currentStock = feedQuantities.get(selectedPos);
        String unit = feedUnits.get(selectedPos);
        String targetGroup = etTargetGroup.getText().toString().trim();
        String notes = etFeedNotes.getText().toString().trim();

        if (consumeQty > currentStock) {
            Toast.makeText(this, "Insufficient feed stock (Current: " + currentStock + " " + unit + ")", Toast.LENGTH_LONG).show();
            return;
        }

        double newStock = currentStock - consumeQty;
        String todayDate = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(new Date());

        android.database.sqlite.SQLiteDatabase db = getDb();
        if (db == null) return;

        db.beginTransaction();
        try {
            // Update stock level
            ContentValues invValues = new ContentValues();
            invValues.put("quantity", newStock);
            db.update("inventory", invValues, "id=?", new String[]{String.valueOf(feedId)});

            // Record transaction log
            ContentValues transValues = new ContentValues();
            transValues.put("item_id", feedId);
            transValues.put("type", "Stock Out");
            transValues.put("quantity", consumeQty);
            transValues.put("date", todayDate);
            transValues.put("remarks", "Consumed by: " + (targetGroup.isEmpty() ? "Herd" : targetGroup) + ". " + notes);
            long transId = db.insert("inventory_transactions", null, transValues);

            // Record feed consumption entry
            ContentValues fcValues = new ContentValues();
            fcValues.put("feed_id", feedId);
            fcValues.put("date", todayDate);
            fcValues.put("quantity", consumeQty);
            fcValues.put("cost", 0);
            fcValues.put("group_or_animal_id", targetGroup);
            fcValues.put("notes", notes);
            db.insert("feed_consumption", null, fcValues);

            db.setTransactionSuccessful();
            DatabaseHelper.logAudit(db, "RECORD_FEED_CONSUMPTION", "INVENTORY", transId, "Consumed " + consumeQty + " " + unit + " of Feed ID: " + feedId);

            Toast.makeText(this, "Feed Consumption Recorded", Toast.LENGTH_SHORT).show();

            etConsumeQuantity.setText("");
            etTargetGroup.setText("");
            etFeedNotes.setText("");

            loadFeeds();
        } catch (Exception e) {
            Toast.makeText(this, "Error recording consumption: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
