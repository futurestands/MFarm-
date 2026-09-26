package dev.mfarm.com.mfarm;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import dev.mfarm.com.mfarm.adapters.MenuListAdapter;
import dev.mfarm.com.mfarm.models.menulist;

public class MedicationActivity extends AppCompatActivity {
    List<menulist> supplierList = new ArrayList<>();
    MenuListAdapter aAdpt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Health & Medication");
        }

        final ListView lv = findViewById(R.id.listView);
        lv.setBackgroundColor(Color.TRANSPARENT);

        initList();

        aAdpt = new MenuListAdapter(this, supplierList);
        lv.setAdapter(aAdpt);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                menulist m = aAdpt.getItem(position);
                String rec = m.getId();
                if (rec.equalsIgnoreCase("1")) {
                    startActivity(new Intent(getApplicationContext(), AnimalDoctorActivity.class));
                } else if (rec.equalsIgnoreCase("2")) {
                    startActivity(new Intent(getApplicationContext(), IllnessActivity.class));
                } else if (rec.equalsIgnoreCase("3")) {
                    Intent p = new Intent(getApplicationContext(), IllnessActivity.class);
                    p.putExtra("type", "Mastitis");
                    startActivity(p);
                } else if (rec.equalsIgnoreCase("4")) {
                    startActivity(new Intent(getApplicationContext(), TreatmentActivity.class));
                } else if (rec.equalsIgnoreCase("5")) {
                    startActivity(new Intent(getApplicationContext(), VetCheckActivity.class));
                }
            }
        });
    }

    private void initList() {
        supplierList.clear();
        supplierList.add(new menulist("1", "🩺 Animal Doctor / Veterinary Guide", "img"));
        supplierList.add(new menulist("2", "Illness Occurrence", "img"));
        supplierList.add(new menulist("3", "Mastitis Occurrence", "img"));
        supplierList.add(new menulist("4", "Treatment Records", "img"));
        supplierList.add(new menulist("5", "Vet Checks", "img"));
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
