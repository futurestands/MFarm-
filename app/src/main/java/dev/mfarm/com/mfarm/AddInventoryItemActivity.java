package dev.mfarm.com.mfarm;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

public class AddInventoryItemActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etCategory, etQuantity, etUnit, etMinQuantity;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_inventory_item);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Inventory Item");
        }

        etItemName = findViewById(R.id.etItemName);
        etCategory = findViewById(R.id.etCategory);
        etQuantity = findViewById(R.id.etQuantity);
        etUnit = findViewById(R.id.etUnit);
        etMinQuantity = findViewById(R.id.etMinQuantity);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveItem();
            }
        });
    }

    private void saveItem() {
        String name = etItemName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();
        String minQuantityStr = etMinQuantity.getText().toString().trim();

        if (name.isEmpty() || quantityStr.isEmpty() || unit.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double quantity = Double.parseDouble(quantityStr);
        double minQuantity = minQuantityStr.isEmpty() ? 0 : Double.parseDouble(minQuantityStr);

        ContentValues values = new ContentValues();
        values.put("item_name", name);
        values.put("category", category);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("min_quantity", minQuantity);

        try {
            android.database.sqlite.SQLiteDatabase db = dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(this);
            if (db != null) {
                db.insert("inventory", null, values);
            }
            Toast.makeText(this, "Item Saved Successfully", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
