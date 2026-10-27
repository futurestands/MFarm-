package dev.mfarm.com.mfarm;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.dao.AndroidDatabaseAdapter;
import dev.mfarm.com.mfarm.dao.BatchManager;
import dev.mfarm.com.mfarm.dao.DatabaseAdapter;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class BatchListActivity extends AppCompatActivity {

    private ListView lvBatches;
    private FloatingActionButton fabAddBatch;
    private List<String> batchDisplayList = new ArrayList<>();
    private List<String> batchIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Livestock Batches & Flocks");
        }

        lvBatches = findViewById(R.id.lvBatches);
        fabAddBatch = findViewById(R.id.fabAddBatch);

        fabAddBatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BatchListActivity.this, BatchRegistrationActivity.class));
            }
        });

        lvBatches.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (batchIds != null && position >= 0 && position < batchIds.size()) {
                    showBatchActionDialog(batchIds.get(position));
                }
            }
        });

        loadBatches();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBatches();
    }

    private void loadBatches() {
        batchDisplayList.clear();
        batchIds.clear();

        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(this);
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT batch_id, species, subtype, current_quantity, location_name, status FROM livestock_batches ORDER BY id DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String bId = cursor.getString(0);
                String sp = cursor.getString(1);
                String sub = cursor.getString(2);
                int qty = cursor.getInt(3);
                String loc = cursor.getString(4);
                String status = cursor.getString(5);

                batchIds.add(bId);
                batchDisplayList.add(bId + " — " + sub + " (" + sp + ")\nQuantity: " + qty + " | Location: " + (loc != null ? loc : "Main Farm") + " [" + status + "]");
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();

        if (batchDisplayList.isEmpty()) {
            batchDisplayList.add("No livestock batches registered yet.\nTap '+' to register a batch (e.g. 500 chicks).");
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, batchDisplayList);
        lvBatches.setAdapter(adapter);
    }

    private void showBatchActionDialog(final String batchId) {
        String[] actions = {"Record Mortality / Death", "Record Batch Sale", "Convert 1 to Individual Tracking", "Log Egg Production"};

        new AlertDialog.Builder(this)
                .setTitle("Batch Actions: " + batchId)
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                        String todayStr = sdf.format(new Date());
                        android.database.sqlite.SQLiteDatabase db = DatabaseHelper.getDatabase(BatchListActivity.this);
                        if (db == null) return;
                        DatabaseAdapter adapterDb = new AndroidDatabaseAdapter(db);

                        if (which == 0) { // Record Mortality
                            promptQuantityDialog("Record Mortality", "Enter number of deaths:", new QuantityListener() {
                                @Override
                                public void onConfirm(int qty, String notes) {
                                    BatchManager.recordMortality(adapterDb, batchId, qty, todayStr, "Disease / Natural", notes);
                                    Toast.makeText(BatchListActivity.this, "Mortality recorded: " + qty, Toast.LENGTH_SHORT).show();
                                    loadBatches();
                                }
                            });
                        } else if (which == 1) { // Record Sale
                            promptSaleDialog(batchId, todayStr, adapterDb);
                        } else if (which == 2) { // Convert to Individual
                            promptConvertDialog(batchId, todayStr, adapterDb);
                        } else if (which == 3) { // Log Egg Production
                            promptEggDialog(batchId, todayStr, db);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private interface QuantityListener {
        void onConfirm(int qty, String notes);
    }

    private void promptQuantityDialog(String title, String hint, final QuantityListener listener) {
        final TextInputEditText inputQty = new TextInputEditText(this);
        inputQty.setHint(hint);
        inputQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(inputQty)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String str = inputQty.getText().toString().trim();
                        if (!str.isEmpty()) {
                            listener.onConfirm(Integer.parseInt(str), "Recorded from Batch Manager");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptSaleDialog(final String batchId, final String todayStr, final DatabaseAdapter adapterDb) {
        View view = getLayoutInflater().inflate(R.layout.dialog_batch_sale, null);
        final TextInputEditText etQty = view.findViewById(R.id.etQty);
        final TextInputEditText etPrice = view.findViewById(R.id.etPrice);
        final TextInputEditText etBuyer = view.findViewById(R.id.etBuyer);

        new AlertDialog.Builder(this)
                .setTitle("Record Batch Sale")
                .setView(view)
                .setPositiveButton("Record Sale", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String qStr = etQty.getText().toString().trim();
                        String pStr = etPrice.getText().toString().trim();
                        String buyer = etBuyer.getText().toString().trim();

                        if (!qStr.isEmpty()) {
                            int qty = Integer.parseInt(qStr);
                            double price = pStr.isEmpty() ? 0 : Double.parseDouble(pStr);
                            BatchManager.recordBatchSale(adapterDb, batchId, qty, todayStr, price, buyer, "Batch Sale");
                            Toast.makeText(BatchListActivity.this, "Batch sale recorded!", Toast.LENGTH_SHORT).show();
                            loadBatches();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptConvertDialog(final String batchId, final String todayStr, final DatabaseAdapter adapterDb) {
        final TextInputEditText etTag = new TextInputEditText(this);
        etTag.setHint("Enter Permanent Tag / Name for Individual Animal");

        new AlertDialog.Builder(this)
                .setTitle("Convert 1 to Individual Tracking")
                .setMessage("This will decrease batch quantity by 1 and register an individual animal record:")
                .setView(etTag)
                .setPositiveButton("Convert", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tag = etTag.getText().toString().trim();
                        if (!tag.isEmpty()) {
                            long animId = BatchManager.convertBatchToIndividual(adapterDb, batchId, todayStr, tag, tag, "Female", "Standard", "Converted from batch " + batchId);
                            Toast.makeText(BatchListActivity.this, "Individual Animal Registered (ID: " + animId + ")", Toast.LENGTH_LONG).show();
                            loadBatches();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptEggDialog(final String batchId, final String todayStr, final android.database.sqlite.SQLiteDatabase db) {
        final TextInputEditText etEggs = new TextInputEditText(this);
        etEggs.setHint("Number of Eggs Collected Today");
        etEggs.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Log Daily Egg Production")
                .setView(etEggs)
                .setPositiveButton("Save Log", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String str = etEggs.getText().toString().trim();
                        if (!str.isEmpty()) {
                            int count = Integer.parseInt(str);
                            android.content.ContentValues cv = new android.content.ContentValues();
                            cv.put("batch_id", batchId);
                            cv.put("log_date", todayStr);
                            cv.put("eggs_collected", count);
                            db.insert("egg_production_logs", null, cv);
                            Toast.makeText(BatchListActivity.this, "Logged " + count + " eggs for batch " + batchId, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
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
