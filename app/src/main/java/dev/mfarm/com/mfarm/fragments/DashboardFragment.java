package dev.mfarm.com.mfarm.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dev.mfarm.com.mfarm.AlarmScheduler;
import dev.mfarm.com.mfarm.ExpenseActivity;
import dev.mfarm.com.mfarm.FarmSyncActivity;
import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.MedicationActivity;
import dev.mfarm.com.mfarm.MilkProductionActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.RegisterActivity;
import dev.mfarm.com.mfarm.ScheduleVaccinationActivity;
import dev.mfarm.com.mfarm.sync.FarmSyncManager;

public class DashboardFragment extends Fragment {

    private TextView tvFarmTitle;
    private TextView tvStatAnimals, tvStatMilk, tvStatLowStock, tvStatVaccines;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_layout, container, false);

        TextView banner = view.findViewById(R.id.tvFarmSyncBanner);
        banner.setText(FarmSyncManager.bannerText(getActivity()));
        banner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), FarmSyncActivity.class));
            }
        });

        tvFarmTitle = view.findViewById(R.id.tvFarmTitle);
        tvStatAnimals = view.findViewById(R.id.tvStatAnimals);
        tvStatMilk = view.findViewById(R.id.tvStatMilk);
        tvStatLowStock = view.findViewById(R.id.tvStatLowStock);
        tvStatVaccines = view.findViewById(R.id.tvStatVaccines);

        loadDashboardStats();

        Button imgregister = view.findViewById(R.id.imgregister);
        imgregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), RegisterActivity.class));
            }
        });

        Button imgmilk = view.findViewById(R.id.imgmilk);
        imgmilk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MilkProductionActivity.class));
            }
        });

        Button imgmedication = view.findViewById(R.id.imgmedication);
        imgmedication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MedicationActivity.class));
            }
        });

        Button imgexpenses = view.findViewById(R.id.imgexpenses);
        imgexpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ExpenseActivity.class));
            }
        });

        Button imgvaccination = view.findViewById(R.id.imgvaccination);
        imgvaccination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ScheduleVaccinationActivity.class));
            }
        });

        Button imginventory = view.findViewById(R.id.imginventory);
        imginventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.addToBackStack("I");
                ft.replace(R.id.main_frame, InventoryListFragment.newInstance(), "I");
                ft.commit();
            }
        });

        Button imgbreeding = view.findViewById(R.id.imgbreeding);
        imgbreeding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.addToBackStack("B");
                ft.replace(R.id.main_frame, BreedingListFragment.newInstance(), "B");
                ft.commit();
            }
        });

        return view;
    }

    private void loadDashboardStats() {
        if (MainActivity.database == null || !MainActivity.database.isOpen()) return;

        // Farm Name
        try {
            Cursor cProfile = MainActivity.database.rawQuery("SELECT farm_name FROM farm_profile LIMIT 1", null);
            if (cProfile.moveToFirst() && cProfile.getString(0) != null && !cProfile.getString(0).isEmpty()) {
                tvFarmTitle.setText(cProfile.getString(0));
            } else {
                tvFarmTitle.setText("MFarm Dashboard");
            }
            cProfile.close();
        } catch (Exception ignored) {}

        // Total Animals
        try {
            Cursor cAnimals = MainActivity.database.rawQuery("SELECT COUNT(*) FROM animas WHERE lifecycle_status = 'Active'", null);
            if (cAnimals.moveToFirst()) {
                tvStatAnimals.setText(String.valueOf(cAnimals.getInt(0)));
            }
            cAnimals.close();
        } catch (Exception ignored) {}

        // Today's Milk
        try {
            String today = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
            Cursor cMilk = MainActivity.database.rawQuery("SELECT SUM(litres) FROM milk_production WHERE datetime = ?", new String[]{today});
            if (cMilk.moveToFirst()) {
                double total = cMilk.getDouble(0);
                tvStatMilk.setText(String.format(Locale.US, "%.1f L", total));
            }
            cMilk.close();
        } catch (Exception ignored) {}

        // Low Stock Count
        try {
            Cursor cStock = MainActivity.database.rawQuery("SELECT COUNT(*) FROM inventory WHERE quantity <= min_quantity", null);
            if (cStock.moveToFirst()) {
                tvStatLowStock.setText(String.valueOf(cStock.getInt(0)));
            }
            cStock.close();
        } catch (Exception ignored) {}

        // Pending Vaccinations Count
        try {
            AlarmScheduler.updateOverdueStatus(MainActivity.database);
            Cursor cVac = MainActivity.database.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Pending' OR status = 'Overdue'", null);
            if (cVac.moveToFirst()) {
                tvStatVaccines.setText(String.valueOf(cVac.getInt(0)));
            }
            cVac.close();
        } catch (Exception ignored) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            TextView banner = view.findViewById(R.id.tvFarmSyncBanner);
            if (banner != null && getActivity() != null) {
                banner.setText(FarmSyncManager.bannerText(getActivity()));
            }
            loadDashboardStats();
        }
    }
}
