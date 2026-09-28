package dev.mfarm.com.mfarm;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.MenuItem;
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
import java.util.Calendar;
import java.util.Locale;

public class StockTransactionActivity extends AppCompatActivity {

    private TextView tvCurrentStock;
    private Spinner spinnerType;
    private TextInputEditText etTransQuantity, etTransRemarks;
    private Button btnSaveTransaction;

    private int itemId;
    private String itemName;
    private double currentQuantity;
    private String unit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_transaction);

        itemId = getIntent().getIntExtra("item_id", -1);
        itemName = getIntent().getStringExtra("item_name");
        currentQuantity = getIntent().getDoubleExtra("current_quantity", 0);
        unit = getIntent().getStringExtra("unit");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Update Stock: " + itemName);
        }

        tvCurrentStock = findViewById(R.id.tvCurrentStock);
        spinnerType = findViewById(R.id.spinnerType);
        etTransQuantity = findViewById(R.id.etTransQuantity);
        etTransRemarks = findViewById(R.id.etTransRemarks);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);

        tvCurrentStock.setText(String.format("Current Stock: %.2f %s", currentQuantity, unit));

        String[] types = {"Stock In", "Stock Out"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void saveTransaction() {
        String quantityStr = etTransQuantity.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        String remarks = etTransRemarks.getText().toString().trim();

        if (quantityStr.isEmpty()) {
            Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        double transQuantity = Double.parseDouble(quantityStr);
        double newQuantity = type.equals("Stock In") ? currentQuantity + transQuantity : currentQuantity - transQuantity;

        if (newQuantity < 0) {
            Toast.makeText(this, "Insufficient stock", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());

        ContentValues transValues = new ContentValues();
        transValues.put("item_id", itemId);
        transValues.put("type", type);
        transValues.put("quantity", transQuantity);
        transValues.put("date", date);
        transValues.put("remarks", remarks);

        ContentValues invValues = new ContentValues();
        invValues.put("quantity", newQuantity);

        android.database.sqlite.SQLiteDatabase db = dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(this);
        if (db == null) return;

        try {
            db.beginTransaction();
            db.insert("inventory_transactions", null, transValues);
            db.update("inventory", invValues, "id=?", new String[]{String.valueOf(itemId)});
            db.setTransactionSuccessful();
            Toast.makeText(this, "Stock Updated Successfully", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
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
