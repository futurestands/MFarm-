package dev.mfarm.com.mfarm.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import dev.mfarm.com.mfarm.AlarmScheduler;
import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.adapters.VaccinationAdapter;
import dev.mfarm.com.mfarm.models.Vaccination;

public class VaccinationListFragment extends Fragment {

    private List<Vaccination> vaccinationList = new ArrayList<>();
    private VaccinationAdapter adapter;

    public static VaccinationListFragment newInstance() {
        return new VaccinationListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vaccination_list, container, false);
        ListView listView = view.findViewById(R.id.listViewVaccinations);

        final android.widget.TextView emptyView = new android.widget.TextView(getActivity());
        emptyView.setText("No vaccinations scheduled.");
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(20, 20, 20, 20);
        ((ViewGroup)listView.getParent()).addView(emptyView);
        listView.setEmptyView(emptyView);

        loadVaccinations();

        adapter = new VaccinationAdapter(getActivity(), vaccinationList);
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Vaccination v = vaccinationList.get(position);
                if ("Completed".equals(v.getStatus())) return false;

                new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                        .setTitle("Mark as Completed")
                        .setMessage("Have you completed the " + v.getVaccineName() + " for " + v.getAnimalName() + "?")
                        .setPositiveButton("Yes", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                android.content.ContentValues values = new android.content.ContentValues();
                                values.put("status", "Completed");
                                MainActivity.database.update("vaccinations", values, "id=?", new String[]{String.valueOf(v.getId())});
                                AlarmScheduler.cancel(getActivity(), v.getId());
                                AlarmScheduler.clearNotified(getActivity(), v.getId());
                                loadVaccinations();
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });

        return view;
    }

    private void loadVaccinations() {
        vaccinationList.clear();
        AlarmScheduler.updateOverdueStatus(MainActivity.database);
        String sql = "SELECT v.*, a.name FROM vaccinations v " +
                     "JOIN animas a ON v.animal_id = a.id " +
                     "ORDER BY v.id DESC";
        
        Cursor cursor = MainActivity.database.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                Vaccination v = new Vaccination(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                );
                v.setAnimalName(cursor.getString(6)); // Join result
                vaccinationList.add(v);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}
