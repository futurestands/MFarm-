package dev.mfarm.com.mfarm.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.models.Vaccination;

public class VaccinationAdapter extends ArrayAdapter<Vaccination> {
    private Context context;
    private List<Vaccination> list;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    public VaccinationAdapter(Context context, List<Vaccination> list) {
        super(context, R.layout.vaccination_row_layout, list);
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.vaccination_row_layout, parent, false);
        }

        Vaccination item = list.get(position);

        TextView tvAnimalName = convertView.findViewById(R.id.tvAnimalName);
        TextView tvStatus = convertView.findViewById(R.id.tvStatus);
        TextView tvVaccineName = convertView.findViewById(R.id.tvVaccineName);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvRemarks = convertView.findViewById(R.id.tvRemarks);

        tvAnimalName.setText(item.getAnimalName());
        tvVaccineName.setText(item.getVaccineName());
        tvDate.setText(item.getScheduledDate());
        tvRemarks.setText(item.getRemarks());

        if ("Completed".equalsIgnoreCase(item.getStatus())) {
            tvStatus.setText("COMPLETED");
            tvStatus.setTextColor(Color.WHITE);
            tvStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
        } else if ("Overdue".equalsIgnoreCase(item.getStatus())) {
            tvStatus.setText("OVERDUE");
            tvStatus.setTextColor(Color.WHITE);
            tvStatus.setBackgroundColor(Color.parseColor("#F44336")); // Red
        } else {
            boolean overdue = false;
            try {
                Date scheduled = sdf.parse(item.getScheduledDate());
                Date today = sdf.parse(sdf.format(new Date()));
                if (scheduled != null && scheduled.before(today)) {
                    overdue = true;
                }
            } catch (Exception ignored) {}

            if (overdue) {
                tvStatus.setText("OVERDUE");
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setBackgroundColor(Color.parseColor("#F44336")); // Red
            } else {
                tvStatus.setText("UPCOMING");
                tvStatus.setTextColor(Color.WHITE);
                tvStatus.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
            }
        }

        return convertView;
    }
}
