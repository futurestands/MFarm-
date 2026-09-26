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
import dev.mfarm.com.mfarm.models.animal;
import dev.mfarm.com.mfarm.models.menulist;

/**
 * Created by SIMGICH on 7/25/2016.
 */
public class MenuListAdapter extends ArrayAdapter<menulist> implements Filterable {
    private Context context;
    private Filter supplierFilter;
    private List<menulist> supplierList;
    private List<menulist> origsupplierList;

    public MenuListAdapter(Context ctx, List<menulist> supplierList) {
        super(ctx, R.layout.img_list_view, supplierList);
        // TODO Auto-generated constructor stub
        this.supplierList = supplierList;
        this.context = ctx;
        this.origsupplierList = supplierList;
    }


    public int getCount() {
        return supplierList.size();
    }

    public menulist getItem(int position) {
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
            v = LayoutInflater.from(context).inflate(R.layout.img_list_view, parent, false);
            // Now we can fill the layout with the right values
            TextView tv = (TextView) v.findViewById(R.id.name);
            //TextView distView = (TextView) v.findViewById(R.id.dist);


            holder.planetNameView = tv;
            //holder.distView = distView;

            v.setTag(holder);
        }
        else
            holder = (SupplierHolder) v.getTag();

        menulist sup = supplierList.get(position);
        holder.planetNameView.setText(sup.getMenuitem());
        //holder.distView.setText("Age " + sup.getDob() +"   Breed:["+ sup.getBreed_id()+"]");
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
