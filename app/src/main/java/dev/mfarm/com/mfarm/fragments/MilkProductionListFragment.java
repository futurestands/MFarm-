package dev.mfarm.com.mfarm.fragments;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.adapters.MilkProductionAdapter;
import dev.mfarm.com.mfarm.models.milkproduction;

public class MilkProductionListFragment extends Fragment {
    private List<milkproduction> supplierList = new ArrayList<>();
    private MilkProductionAdapter aAdpt;
    private TextView tvTodayYield, tvAvgYield;

    public static MilkProductionListFragment newInstance() {
        return new MilkProductionListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_milk_list, container, false);

        tvTodayYield = view.findViewById(R.id.tvTodayYield);
        tvAvgYield = view.findViewById(R.id.tvAvgYield);

        final ListView lv = view.findViewById(R.id.listView);
        final TextView emptyView = new TextView(getActivity());
        emptyView.setText("No milk records found.");
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(20, 20, 20, 20);
        ((ViewGroup) lv.getParent()).addView(emptyView);
        lv.setEmptyView(emptyView);

        lv.setBackgroundColor(Color.TRANSPARENT);

        loadStats();
        initList();

        aAdpt = new MilkProductionAdapter(getActivity(), supplierList);
        lv.setAdapter(aAdpt);
        return view;
    }

    private void loadStats() {
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        double totalToday = 0;
        Cursor c1 = MainActivity.database.rawQuery("SELECT SUM(litres) FROM milk_production WHERE datetime = ?", new String[]{today});
        if (c1.moveToFirst()) {
            totalToday = c1.getDouble(0);
        }
        c1.close();

        int cowsCount = 1;
        Cursor c2 = MainActivity.database.rawQuery("SELECT COUNT(DISTINCT animal_id) FROM milk_production WHERE datetime = ?", new String[]{today});
        if (c2.moveToFirst()) {
            cowsCount = c2.getInt(0);
            if (cowsCount == 0) cowsCount = 1;
        }
        c2.close();

        tvTodayYield.setText(String.format(Locale.US, "%.1f L", totalToday));
        tvAvgYield.setText(String.format(Locale.US, "%.1f L", totalToday / cowsCount));
    }

    private void initList() {
        supplierList.clear();
        Cursor c = MainActivity.database.rawQuery("SELECT m.id, m.animal_id, m.litres, m.datetime, m.description, a.name FROM milk_production m JOIN animas a ON m.animal_id = a.id ORDER BY m.id DESC", null);
        if (c.moveToFirst()) {
            do {
                supplierList.add(new milkproduction(
                        c.getString(0), c.getString(1), c.getString(2),
                        c.getString(4), c.getString(3), c.getString(5)
                ));
            } while (c.moveToNext());
        }
        c.close();
    }
}
