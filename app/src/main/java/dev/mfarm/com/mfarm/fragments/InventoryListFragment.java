package dev.mfarm.com.mfarm.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import dev.mfarm.com.mfarm.AddInventoryItemActivity;
import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.StockTransactionActivity;
import dev.mfarm.com.mfarm.adapters.InventoryAdapter;
import dev.mfarm.com.mfarm.models.InventoryItem;

public class InventoryListFragment extends Fragment {

    private List<InventoryItem> itemList = new ArrayList<>();
    private InventoryAdapter adapter;

    public static InventoryListFragment newInstance() {
        return new InventoryListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory_list, container, false);
        ListView listView = view.findViewById(R.id.listViewInventory);
        Button btnAddItem = view.findViewById(R.id.btnAddItem);
        TextView emptyView = view.findViewById(R.id.emptyView);

        if (emptyView != null) {
            listView.setEmptyView(emptyView);
        }

        loadInventory();

        adapter = new InventoryAdapter(getActivity(), itemList);
        listView.setAdapter(adapter);

        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddInventoryItemActivity.class);
                startActivity(intent);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InventoryItem item = itemList.get(position);
                Intent intent = new Intent(getActivity(), StockTransactionActivity.class);
                intent.putExtra("item_id", item.getId());
                intent.putExtra("item_name", item.getItemName());
                intent.putExtra("current_quantity", item.getQuantity());
                intent.putExtra("unit", item.getUnit());
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInventory();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadInventory() {
        itemList.clear();
        try {
            Cursor cursor = MainActivity.database.rawQuery("SELECT * FROM inventory ORDER BY item_name ASC", null);
            if (cursor.moveToFirst()) {
                do {
                    InventoryItem item = new InventoryItem(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getDouble(3),
                            cursor.getString(4),
                            cursor.getDouble(5)
                    );
                    itemList.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception ignored) {}
    }
}
