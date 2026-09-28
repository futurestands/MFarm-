package dev.mfarm.com.mfarm;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;
import dev.mfarm.com.mfarm.fragments.AnimalListFragment;
import dev.mfarm.com.mfarm.fragments.BreedingListFragment;
import dev.mfarm.com.mfarm.fragments.DashboardFragment;
import dev.mfarm.com.mfarm.fragments.ExpenseListFragment;
import dev.mfarm.com.mfarm.fragments.FinancialDashboardFragment;
import dev.mfarm.com.mfarm.fragments.InventoryListFragment;
import dev.mfarm.com.mfarm.fragments.MilkProductionListFragment;
import dev.mfarm.com.mfarm.fragments.VaccinationListFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    AnimalListFragment _fragAnimalList;
    MilkProductionListFragment _fragMedication;
    ExpenseListFragment _fragExpenseList;
    VaccinationListFragment _fragVaccinationList;
    InventoryListFragment _fragInventoryList;
    BreedingListFragment _fragBreedingList;
    FinancialDashboardFragment _fragFinDash;
    DashboardFragment _fragDashboard;

    public static final String DB_NAME = "farmapp.sqlite";
    public static SQLiteDatabase database;
    static DatabaseHelper dbOpenHelper;

    public static SQLiteDatabase getDb(Context context) {
        SQLiteDatabase db = DatabaseHelper.getDatabase(context);
        database = db;
        return db;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            database = DatabaseHelper.getDatabase(this);
            if (database != null) {
                AlarmScheduler.reschedulePending(this);
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Could not open farm database", android.widget.Toast.LENGTH_LONG).show();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            showDashboard();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dev.mfarm.com.mfarm.sync.FarmSyncManager.autoSync(getApplicationContext());
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private void showDashboard() {
        _fragDashboard = DashboardFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_frame, _fragDashboard, "D")
                .commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (id == R.id.nav_homr) {
            showDashboard();
        } else if (id == R.id.nav_search) {
            startActivity(new Intent(this, SearchActivity.class));
        } else if (id == R.id.nav_camera) {
            _fragAnimalList = AnimalListFragment.newInstance();
            getSupportFragmentManager().beginTransaction().addToBackStack("A").replace(R.id.main_frame, _fragAnimalList, "A").commit();
        } else if (id == R.id.nav_gallery) {
            _fragMedication = MilkProductionListFragment.newInstance();
            getSupportFragmentManager().beginTransaction().addToBackStack("M").replace(R.id.main_frame, _fragMedication, "M").commit();
        } else if (id == R.id.nav_slideshow) {
            startActivity(new Intent(this, MedicationActivity.class));
        } else if (id == R.id.nav_manage) {
            _fragBreedingList = BreedingListFragment.newInstance();
            getSupportFragmentManager().beginTransaction().addToBackStack("B").replace(R.id.main_frame, _fragBreedingList, "B").commit();
        } else if (id == R.id.nav_feed) {
            startActivity(new Intent(this, FeedManagementActivity.class));
        } else if (id == R.id.nav_income) {
            startActivity(new Intent(this, IncomeActivity.class));
        } else if (id == R.id.nav_expenses) {
            _fragExpenseList = ExpenseListFragment.newInstance();
            getSupportFragmentManager().beginTransaction().addToBackStack("E").replace(R.id.main_frame, _fragExpenseList, "E").commit();
        } else if (id == R.id.nav_fin_dash) {
            _fragFinDash = FinancialDashboardFragment.newInstance();
            getSupportFragmentManager().beginTransaction().addToBackStack("F").replace(R.id.main_frame, _fragFinDash, "F").commit();
        } else if (id == R.id.nav_vaccination) {
            _fragVaccinationList = VaccinationListFragment.newInstance();
            getSupportFragmentManager().beginTransaction().addToBackStack("V").replace(R.id.main_frame, _fragVaccinationList, "V").commit();
        } else if (id == R.id.nav_inventory) {
            _fragInventoryList = InventoryListFragment.newInstance();
            getSupportFragmentManager().beginTransaction().addToBackStack("I").replace(R.id.main_frame, _fragInventoryList, "I").commit();
        } else if (id == R.id.nav_send) {
            startActivity(new Intent(this, ExportActivity.class));
        } else if (id == R.id.nav_audit) {
            startActivity(new Intent(this, AuditLogActivity.class));
        } else if (id == R.id.nav_farm_sync) {
            startActivity(new Intent(this, FarmSyncActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
