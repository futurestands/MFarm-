package dev.mfarm.com.mfarm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.models.InventoryItem;

public class InventoryAdapter extends ArrayAdapter<InventoryItem> {
    private Context context;
    private List<InventoryItem> itemList;

    public InventoryAdapter(Context context, List<InventoryItem> itemList) {
        super(context, R.layout.inventory_row_layout, itemList);
        this.context = context;
        this.itemList = itemList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.inventory_row_layout, parent, false);
        }

        InventoryItem item = itemList.get(position);

        TextView tvItemName = convertView.findViewById(R.id.tvItemName);
        TextView tvQuantity = convertView.findViewById(R.id.tvQuantity);
        TextView tvCategory = convertView.findViewById(R.id.tvCategory);
        TextView tvLowStockWarning = convertView.findViewById(R.id.tvLowStockWarning);

        tvItemName.setText(item.getItemName());
        tvQuantity.setText(String.format("%.2f %s", item.getQuantity(), item.getUnit()));
        tvCategory.setText(item.getCategory());

        if (item.getQuantity() <= item.getMinQuantity()) {
            tvLowStockWarning.setVisibility(View.VISIBLE);
        } else {
            tvLowStockWarning.setVisibility(View.GONE);
        }

        return convertView;
    }
}
