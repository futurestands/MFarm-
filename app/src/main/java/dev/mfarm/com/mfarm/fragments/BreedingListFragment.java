package dev.mfarm.com.mfarm.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import dev.mfarm.com.mfarm.AddBreedingActivity;
import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.adapters.BreedingAdapter;
import dev.mfarm.com.mfarm.models.BreedingRecord;

public class BreedingListFragment extends Fragment {

    private List<BreedingRecord> breedingList = new ArrayList<>();
    private BreedingAdapter adapter;

    public static BreedingListFragment newInstance() {
        return new BreedingListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_breeding_list, container, false);
        ListView listView = view.findViewById(R.id.listViewBreeding);
        Button btnAdd = view.findViewById(R.id.btnAddBreeding);
        TextView emptyView = view.findViewById(R.id.emptyView);

        if (emptyView != null) {
            listView.setEmptyView(emptyView);
        }

        loadRecords();

        adapter = new BreedingAdapter(getActivity(), breedingList);
        listView.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), AddBreedingActivity.class));
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecords();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadRecords() {
        breedingList.clear();
        String sql = "SELECT b.*, a.name FROM breeding_records b " +
                     "JOIN animas a ON b.animal_id = a.id " +
                     "ORDER BY b.id DESC";
        
        try {
            android.database.sqlite.SQLiteDatabase db = dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(getContext() != null ? getContext() : getActivity());
            if (db == null) return;

            Cursor cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
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
            if (cursor != null) cursor.close();
        } catch (Exception ignored) {}
    }
}
