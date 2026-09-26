package dev.mfarm.com.mfarm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.models.BreedingRecord;

public class BreedingAdapter extends ArrayAdapter<BreedingRecord> {
    private Context context;
    private List<BreedingRecord> list;

    public BreedingAdapter(Context context, List<BreedingRecord> list) {
        super(context, R.layout.breeding_row_layout, list);
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.breeding_row_layout, parent, false);
        }

        BreedingRecord item = list.get(position);

        TextView tvAnimalName = convertView.findViewById(R.id.tvAnimalName);
        TextView tvStatus = convertView.findViewById(R.id.tvStatus);
        TextView tvDates = convertView.findViewById(R.id.tvDates);
        TextView tvBullInfo = convertView.findViewById(R.id.tvBullInfo);

        tvAnimalName.setText(item.getAnimalName());
        tvStatus.setText(item.getStatus());
        tvDates.setText("Mating: " + item.getMatingDate() + " | Birth: " + item.getExpectedBirthDate());
        tvBullInfo.setText("Bull: " + item.getBullId());

        return convertView;
    }
}
