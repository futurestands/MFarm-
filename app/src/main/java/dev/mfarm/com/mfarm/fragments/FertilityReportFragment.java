package dev.mfarm.com.mfarm.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.adapters.BreedingAdapter;
import dev.mfarm.com.mfarm.models.BreedingRecord;

public class FertilityReportFragment extends Fragment {

    private List<BreedingRecord> breedingList = new ArrayList<>();
    private BreedingAdapter adapter;
    private TextView tvPregnantCount, tvUpcomingCalving;

    public static FertilityReportFragment newInstance() {
        return new FertilityReportFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fertility_report, container, false);
        
        tvPregnantCount = view.findViewById(R.id.tvPregnantCount);
        tvUpcomingCalving = view.findViewById(R.id.tvUpcomingCalving);
        ListView listView = view.findViewById(R.id.listViewRecentBreeding);

        loadStats();
        loadRecentActivity();

        adapter = new BreedingAdapter(getActivity(), breedingList);
        listView.setAdapter(adapter);

        return view;
    }

    private void loadStats() {
        // Count Pregnant
        Cursor c1 = MainActivity.database.rawQuery("SELECT COUNT(*) FROM breeding_records WHERE status = 'Pregnant'", null);
        if (c1.moveToFirst()) {
            tvPregnantCount.setText("Total Pregnant Cows: " + c1.getInt(0));
        }
        c1.close();

        // Count upcoming calving in next 30 days
        // We'll use a simple SQL string comparison if date format allows, or fetch and check in Java
        // For robustness, let's fetch all and filter
        int upcoming = 0;
        Cursor c2 = MainActivity.database.rawQuery("SELECT expected_birth_date FROM breeding_records WHERE status = 'Pregnant'", null);
        if (c2.moveToFirst()) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_YEAR, 30);
            long next30Days = cal.getTimeInMillis();
            long now = System.currentTimeMillis();

            do {
                try {
                    String dStr = c2.getString(0);
                    java.util.Date d = sdf.parse(dStr);
                    if (d != null && d.getTime() >= now && d.getTime() <= next30Days) {
                        upcoming++;
                    }
                } catch (Exception e) {}
            } while (c2.moveToNext());
        }
        c2.close();
        tvUpcomingCalving.setText("Calving in next 30 days: " + upcoming);
    }

    private void loadRecentActivity() {
        breedingList.clear();
        String sql = "SELECT b.*, a.name FROM breeding_records b " +
                     "JOIN animas a ON b.animal_id = a.id " +
                     "ORDER BY b.id DESC LIMIT 5";
        
        Cursor cursor = MainActivity.database.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                BreedingRecord b = new BreedingRecord(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                );
                b.setAnimalName(cursor.getString(6));
                breedingList.add(b);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}
