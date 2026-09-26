package dev.mfarm.com.mfarm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.List;


import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.models.milkproduction;

/**
 * Created by SIMGICH on 4/20/2016.
 */
public class MilkProductionAdapter  extends ArrayAdapter<milkproduction> implements Filterable {
    private Context context;
    private Filter supplierFilter;
    private List<milkproduction> supplierList;
    private List<milkproduction> origsupplierList;

    public MilkProductionAdapter(Context ctx, List<milkproduction> supplierList) {
        super(ctx, R.layout.img_row_layout, supplierList);
        // TODO Auto-generated constructor stub
        this.supplierList = supplierList;
        this.context = ctx;
        this.origsupplierList = supplierList;
    }

    public int getCount() {
        return supplierList.size();
    }

    public milkproduction getItem(int position) {
        return supplierList.get(position);
    }

    public long getItemId(int position) {
        return supplierList.get(position).hashCode();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        SupplierHolder holder = new SupplierHolder();

        // First let's verify the convertView is not null
        if (convertView == null) {
            // This a new view we inflate the new layout
            v = LayoutInflater.from(context).inflate(R.layout.img_row_layout, parent, false);
            // Now we can fill the layout with the right values
            TextView tv = (TextView) v.findViewById(R.id.name);
            TextView distView = (TextView) v.findViewById(R.id.dist);


            holder.planetNameView = tv;
            holder.distView = distView;

            v.setTag(holder);
        }
        else
            holder = (SupplierHolder) v.getTag();

        milkproduction sup = supplierList.get(position);
        holder.planetNameView.setText(sup.getA_name());
        holder.distView.setText("Litres " + sup.getLitres() +"   Date:["+ sup.getDatetime()+"]");
        //holder.distView.setText("" + sup.getNat_id());
        return v;
    }

    public void resetData() {
        supplierList = origsupplierList;
    }


    private static class SupplierHolder{
        public TextView planetNameView;
        public TextView distView;
    }

}
