package dev.mfarm.com.mfarm.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.AlarmScheduler;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;
import dev.mfarm.com.mfarm.ExpenseActivity;
import dev.mfarm.com.mfarm.FarmSyncActivity;
import dev.mfarm.com.mfarm.IncomeActivity;
import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.MedicationActivity;
import dev.mfarm.com.mfarm.MilkProductionActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.RegisterActivity;
import dev.mfarm.com.mfarm.ScheduleVaccinationActivity;
import dev.mfarm.com.mfarm.intelligence.FarmBrief;
import dev.mfarm.com.mfarm.intelligence.FarmIntelligenceEngine;
import dev.mfarm.com.mfarm.intelligence.Insight;
import dev.mfarm.com.mfarm.intelligence.WhatChanged;
import dev.mfarm.com.mfarm.sync.FarmSyncManager;

public class DashboardFragment extends Fragment {

    private TextView tvFarmTitle;
    private TextView tvStatAnimals, tvStatMilk, tvStatLowStock, tvStatVaccines;
    private TextView tvBriefGreeting;
    private LinearLayout layoutBriefContent;
    private LinearLayout layoutWhatChangedContent;
    private LinearLayout layoutInsightsContent;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_layout, container, false);

        TextView banner = view.findViewById(R.id.tvFarmSyncBanner);
        banner.setText(FarmSyncManager.bannerText(getActivity()));
        banner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), FarmSyncActivity.class));
            }
        });

        tvFarmTitle = view.findViewById(R.id.tvFarmTitle);
        tvStatAnimals = view.findViewById(R.id.tvStatAnimals);
        tvStatMilk = view.findViewById(R.id.tvStatMilk);
        tvStatLowStock = view.findViewById(R.id.tvStatLowStock);
        tvStatVaccines = view.findViewById(R.id.tvStatVaccines);

        tvBriefGreeting = view.findViewById(R.id.tvBriefGreeting);
        layoutBriefContent = view.findViewById(R.id.layoutBriefContent);
        layoutWhatChangedContent = view.findViewById(R.id.layoutWhatChangedContent);
        layoutInsightsContent = view.findViewById(R.id.layoutInsightsContent);

        loadDashboardStats();

        Button imgregister = view.findViewById(R.id.imgregister);
        imgregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), RegisterActivity.class));
            }
        });

        Button imgmilk = view.findViewById(R.id.imgmilk);
        imgmilk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MilkProductionActivity.class));
            }
        });

        Button imgmedication = view.findViewById(R.id.imgmedication);
        imgmedication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MedicationActivity.class));
            }
        });

        Button imgexpenses = view.findViewById(R.id.imgexpenses);
        imgexpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ExpenseActivity.class));
            }
        });

        Button imgvaccination = view.findViewById(R.id.imgvaccination);
        imgvaccination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ScheduleVaccinationActivity.class));
            }
        });

        Button imginventory = view.findViewById(R.id.imginventory);
        imginventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.addToBackStack("I");
                ft.replace(R.id.main_frame, InventoryListFragment.newInstance(), "I");
                ft.commit();
            }
        });

        Button imgbreeding = view.findViewById(R.id.imgbreeding);
        imgbreeding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.addToBackStack("B");
                ft.replace(R.id.main_frame, BreedingListFragment.newInstance(), "B");
                ft.commit();
            }
        });

        return view;
    }

    private void loadDashboardStats() {
        SQLiteDatabase db = DatabaseHelper.getDatabase(getContext());
        if (db == null || !db.isOpen()) return;

        // Farm Name
        try {
            Cursor cProfile = db.rawQuery("SELECT farm_name FROM farm_profile LIMIT 1", null);
            if (cProfile.moveToFirst() && cProfile.getString(0) != null && !cProfile.getString(0).isEmpty()) {
                tvFarmTitle.setText(cProfile.getString(0));
            } else {
                tvFarmTitle.setText("MFarm Dashboard");
            }
            cProfile.close();
        } catch (Exception ignored) {}

        // Total Animals
        try {
            Cursor cAnimals = db.rawQuery("SELECT COUNT(*) FROM animas WHERE lifecycle_status = 'Active'", null);
            if (cAnimals.moveToFirst()) {
                tvStatAnimals.setText(String.valueOf(cAnimals.getInt(0)));
            }
            cAnimals.close();
        } catch (Exception ignored) {}

        // Today's Milk
        try {
            String today = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
            Cursor cMilk = db.rawQuery("SELECT SUM(litres) FROM milk_production WHERE datetime = ?", new String[]{today});
            if (cMilk.moveToFirst()) {
                double total = cMilk.getDouble(0);
                tvStatMilk.setText(String.format(Locale.US, "%.1f L", total));
            }
            cMilk.close();
        } catch (Exception ignored) {}

        // Low Stock Count
        try {
            Cursor cStock = db.rawQuery("SELECT COUNT(*) FROM inventory WHERE quantity <= min_quantity", null);
            if (cStock.moveToFirst()) {
                tvStatLowStock.setText(String.valueOf(cStock.getInt(0)));
            }
            cStock.close();
        } catch (Exception ignored) {}

        // Pending Vaccinations Count
        try {
            AlarmScheduler.updateOverdueStatus(db);
            Cursor cVac = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Pending' OR status = 'Overdue'", null);
            if (cVac.moveToFirst()) {
                tvStatVaccines.setText(String.valueOf(cVac.getInt(0)));
            }
            cVac.close();
        } catch (Exception ignored) {}

        // Phase 1 Intelligence: Today's Farm Brief
        loadFarmBrief(db);

        // Phase 1 Intelligence: What Changed Today
        loadWhatChanged(db);

        // Phase 1 Intelligence: Farm Insights
        loadFarmInsights(db);
    }

    private void loadFarmBrief(SQLiteDatabase db) {
        if (layoutBriefContent == null || db == null) return;
        layoutBriefContent.removeAllViews();

        FarmBrief brief = FarmIntelligenceEngine.generateBrief(db);
        if (tvBriefGreeting != null) {
            tvBriefGreeting.setText(brief.getGreeting());
        }

        if (brief.isEmpty()) {
            TextView emptyTv = new TextView(getActivity());
            emptyTv.setText("All farm routines on schedule. No urgent brief items.");
            emptyTv.setTextSize(12);
            emptyTv.setTextColor(Color.parseColor("#424242"));
            layoutBriefContent.addView(emptyTv);
            return;
        }

        if (!brief.getAttentionItems().isEmpty()) {
            addSectionHeader(layoutBriefContent, "Attention Required", "#C62828");
            for (String item : brief.getAttentionItems()) {
                addBulletItem(layoutBriefContent, "• " + item, "#B71C1C");
            }
        }

        if (!brief.getGoodNewsItems().isEmpty()) {
            addSectionHeader(layoutBriefContent, "Good News", "#2E7D32");
            for (String item : brief.getGoodNewsItems()) {
                addBulletItem(layoutBriefContent, "• " + item, "#1B5E20");
            }
        }

        if (!brief.getComingUpItems().isEmpty()) {
            addSectionHeader(layoutBriefContent, "Coming Up", "#1565C0");
            for (String item : brief.getComingUpItems()) {
                addBulletItem(layoutBriefContent, "• " + item, "#0D47A1");
            }
        }
    }

    private void loadWhatChanged(SQLiteDatabase db) {
        if (layoutWhatChangedContent == null || db == null) return;
        layoutWhatChangedContent.removeAllViews();

        WhatChanged changed = FarmIntelligenceEngine.generateWhatChanged(db);
        if (!changed.hasChanges()) {
            TextView noChangeTv = new TextView(getActivity());
            noChangeTv.setText("No significant changes recorded today.");
            noChangeTv.setTextSize(12);
            noChangeTv.setTextColor(Color.parseColor("#616161"));
            layoutWhatChangedContent.addView(noChangeTv);
            return;
        }

        for (String item : changed.getChangesList()) {
            TextView itemTv = new TextView(getActivity());
            itemTv.setText(item);
            itemTv.setTextSize(13);
            itemTv.setPadding(0, 2, 0, 4);
            if (item.startsWith("⚠")) {
                itemTv.setTextColor(Color.parseColor("#C62828"));
                itemTv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                itemTv.setTextColor(Color.parseColor("#333333"));
            }
            layoutWhatChangedContent.addView(itemTv);
        }
    }

    private void loadFarmInsights(SQLiteDatabase db) {
        if (layoutInsightsContent == null || db == null) return;
        layoutInsightsContent.removeAllViews();

        List<Insight> insights = FarmIntelligenceEngine.evaluateInsights(db);
        if (insights.isEmpty()) {
            TextView emptyTv = new TextView(getActivity());
            emptyTv.setText("No farm insights available.");
            emptyTv.setTextSize(12);
            emptyTv.setTextColor(Color.parseColor("#616161"));
            layoutInsightsContent.addView(emptyTv);
            return;
        }

        int maxShow = Math.min(3, insights.size());
        for (int i = 0; i < maxShow; i++) {
            final Insight insight = insights.get(i);

            LinearLayout itemLayout = new LinearLayout(getActivity());
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 0, 0, 10);

            LinearLayout headerLayout = new LinearLayout(getActivity());
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView badgeTv = new TextView(getActivity());
            badgeTv.setText(" " + insight.getPriority().name() + " ");
            badgeTv.setTextSize(10);
            badgeTv.setTypeface(null, android.graphics.Typeface.BOLD);
            badgeTv.setTextColor(Color.WHITE);

            int bgCol = Color.parseColor("#1565C0"); // INFO
            if (insight.getPriority() == Insight.Priority.CRITICAL) {
                bgCol = Color.parseColor("#C62828");
            } else if (insight.getPriority() == Insight.Priority.ATTENTION) {
                bgCol = Color.parseColor("#E65100");
            } else if (insight.getPriority() == Insight.Priority.POSITIVE) {
                bgCol = Color.parseColor("#2E7D32");
            }
            badgeTv.setBackgroundColor(bgCol);
            badgeTv.setPadding(8, 2, 8, 2);

            TextView titleTv = new TextView(getActivity());
            titleTv.setText("  " + insight.getTitle());
            titleTv.setTextSize(13);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            titleTv.setTextColor(Color.parseColor("#212121"));

            headerLayout.addView(badgeTv);
            headerLayout.addView(titleTv);

            TextView msgTv = new TextView(getActivity());
            msgTv.setText(insight.getMessage());
            msgTv.setTextSize(12);
            msgTv.setTextColor(Color.parseColor("#424242"));
            msgTv.setPadding(0, 4, 0, 6);

            itemLayout.addView(headerLayout);
            itemLayout.addView(msgTv);

            if (insight.getActionTarget() != null && !insight.getActionTarget().isEmpty()) {
                itemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleInsightAction(insight.getActionTarget());
                    }
                });
            }

            layoutInsightsContent.addView(itemLayout);
        }
    }

    private void handleInsightAction(String target) {
        if (getActivity() == null) return;
        if ("VACCINATION".equalsIgnoreCase(target)) {
            startActivity(new Intent(getActivity(), ScheduleVaccinationActivity.class));
        } else if ("INVENTORY".equalsIgnoreCase(target)) {
            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.addToBackStack("I");
            ft.replace(R.id.main_frame, InventoryListFragment.newInstance(), "I");
            ft.commit();
        } else if ("MILK".equalsIgnoreCase(target)) {
            startActivity(new Intent(getActivity(), MilkProductionActivity.class));
        } else if ("BREEDING".equalsIgnoreCase(target)) {
            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.addToBackStack("B");
            ft.replace(R.id.main_frame, BreedingListFragment.newInstance(), "B");
            ft.commit();
        } else if ("HEALTH".equalsIgnoreCase(target)) {
            startActivity(new Intent(getActivity(), MedicationActivity.class));
        } else if ("FINANCIAL".equalsIgnoreCase(target)) {
            startActivity(new Intent(getActivity(), IncomeActivity.class));
        } else if ("REGISTER".equalsIgnoreCase(target)) {
            startActivity(new Intent(getActivity(), RegisterActivity.class));
        }
    }

    private void addSectionHeader(LinearLayout container, String title, String colorHex) {
        TextView tv = new TextView(getActivity());
        tv.setText(title);
        tv.setTextSize(11);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setPadding(0, 4, 0, 2);
        container.addView(tv);
    }

    private void addBulletItem(LinearLayout container, String text, String colorHex) {
        TextView tv = new TextView(getActivity());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setPadding(0, 1, 0, 3);
        container.addView(tv);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            TextView banner = view.findViewById(R.id.tvFarmSyncBanner);
            if (banner != null && getActivity() != null) {
                banner.setText(FarmSyncManager.bannerText(getActivity()));
            }
            loadDashboardStats();
        }
    }
}
