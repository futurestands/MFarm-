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
import dev.mfarm.com.mfarm.CropManagementActivity;
import dev.mfarm.com.mfarm.DisposalActivity;
import dev.mfarm.com.mfarm.ExpenseActivity;
import dev.mfarm.com.mfarm.FarmSyncActivity;
import dev.mfarm.com.mfarm.IncomeActivity;
import dev.mfarm.com.mfarm.MedicationActivity;
import dev.mfarm.com.mfarm.MilkProductionActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.RegisterActivity;
import dev.mfarm.com.mfarm.ScheduleVaccinationActivity;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;
import dev.mfarm.com.mfarm.dao.FormatUtils;
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
        if (banner != null) {
            banner.setText(FarmSyncManager.bannerText(getActivity()));
            banner.setOnClickListener(v -> startActivity(new Intent(getActivity(), FarmSyncActivity.class)));
        }

        tvFarmTitle = view.findViewById(R.id.tvFarmTitle);
        tvStatAnimals = view.findViewById(R.id.tvStatAnimals);
        tvStatMilk = view.findViewById(R.id.tvStatMilk);
        tvStatLowStock = view.findViewById(R.id.tvStatLowStock);
        tvStatVaccines = view.findViewById(R.id.tvStatVaccines);

        tvBriefGreeting = view.findViewById(R.id.tvBriefGreeting);
        layoutBriefContent = view.findViewById(R.id.layoutBriefContent);
        layoutWhatChangedContent = view.findViewById(R.id.layoutWhatChangedContent);
        layoutInsightsContent = view.findViewById(R.id.layoutInsightsContent);

        setupQuickActions(view);
        loadDashboardStats(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadDashboardStats(view);
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
            loadDashboardStats(view);
        }
    }

    private void setupQuickActions(View view) {
        Button imgregister = view.findViewById(R.id.imgregister);
        if (imgregister != null) {
            imgregister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));
        }

        Button imgmilk = view.findViewById(R.id.imgmilk);
        if (imgmilk != null) {
            imgmilk.setOnClickListener(v -> startActivity(new Intent(getActivity(), MilkProductionActivity.class)));
        }

        Button imgmedication = view.findViewById(R.id.imgmedication);
        if (imgmedication != null) {
            imgmedication.setOnClickListener(v -> startActivity(new Intent(getActivity(), MedicationActivity.class)));
        }

        Button imgexpenses = view.findViewById(R.id.imgexpenses);
        if (imgexpenses != null) {
            imgexpenses.setOnClickListener(v -> startActivity(new Intent(getActivity(), ExpenseActivity.class)));
        }

        Button imgvaccination = view.findViewById(R.id.imgvaccination);
        if (imgvaccination != null) {
            imgvaccination.setOnClickListener(v -> startActivity(new Intent(getActivity(), ScheduleVaccinationActivity.class)));
        }

        Button imginventory = view.findViewById(R.id.imginventory);
        if (imginventory != null) {
            imginventory.setOnClickListener(v -> {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.addToBackStack("I");
                ft.replace(R.id.main_frame, InventoryListFragment.newInstance(), "I");
                ft.commit();
            });
        }

        Button imgbreeding = view.findViewById(R.id.imgbreeding);
        if (imgbreeding != null) {
            imgbreeding.setOnClickListener(v -> {
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                ft.addToBackStack("B");
                ft.replace(R.id.main_frame, BreedingListFragment.newInstance(), "B");
                ft.commit();
            });
        }
    }

    private void loadDashboardStats(View view) {
        if (view == null) return;
        SQLiteDatabase db = DatabaseHelper.getDatabase(getContext());
        if (db == null || !db.isOpen()) return;

        // Farm Name
        try {
            Cursor cProfile = db.rawQuery("SELECT farm_name FROM farm_profile LIMIT 1", null);
            if (cProfile.moveToFirst() && cProfile.getString(0) != null && !cProfile.getString(0).isEmpty()) {
                if (tvFarmTitle != null) tvFarmTitle.setText(cProfile.getString(0));
            } else {
                if (tvFarmTitle != null) tvFarmTitle.setText("MFarm Dashboard");
            }
            cProfile.close();
        } catch (Exception ignored) {}

        // Total Animals (Individual + Batch Headcount)
        try {
            int indivCount = 0;
            int batchCount = 0;
            Cursor cAnimals = db.rawQuery("SELECT COUNT(*) FROM animas WHERE lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = ''", null);
            if (cAnimals.moveToFirst()) {
                indivCount = cAnimals.getInt(0);
            }
            cAnimals.close();

            Cursor cBatches = db.rawQuery("SELECT SUM(current_quantity) FROM livestock_batches WHERE status = 'Active' OR status IS NULL OR status = ''", null);
            if (cBatches.moveToFirst()) {
                batchCount = cBatches.getInt(0);
            }
            cBatches.close();

            int totalHeadcount = indivCount + batchCount;
            if (tvStatAnimals != null) {
                if (batchCount > 0) {
                    tvStatAnimals.setText(totalHeadcount + " (" + indivCount + " Ind. / " + batchCount + " Batch)");
                } else {
                    tvStatAnimals.setText(String.valueOf(indivCount));
                }
            }

            // Species breakdown
            int cattle = countSpecies(db, "Cattle");
            int goats = countSpecies(db, "Goat");
            int sheep = countSpecies(db, "Sheep");
            int poultry = countSpecies(db, "Poultry");
            int rabbits = countSpecies(db, "Rabbit");

            TextView tvSpecies = view.findViewById(R.id.tvDashSpeciesBreakdown);
            if (tvSpecies != null) {
                tvSpecies.setText(String.format(Locale.US, "Species: %d Cattle • %d Goats • %d Sheep • %d Poultry • %d Rabbits", cattle, goats, sheep, poultry, rabbits));
            }
        } catch (Exception ignored) {}

        // Current Month UGX Cash Flow
        try {
            double mIncome = 0;
            double mExpense = 0;

            java.util.Calendar calToday = java.util.Calendar.getInstance();
            int targetMonth = calToday.get(java.util.Calendar.MONTH);
            int targetYear = calToday.get(java.util.Calendar.YEAR);

            Cursor c1 = db.rawQuery("SELECT date, amount FROM income", null);
            if (c1 != null) {
                while (c1.moveToNext()) {
                    String dStr = c1.getString(0);
                    double amt = c1.getDouble(1);
                    if (isSameMonth(dStr, targetMonth, targetYear)) {
                        mIncome += amt;
                    }
                }
                c1.close();
            }

            Cursor c2 = db.rawQuery("SELECT date, amount FROM expenses", null);
            if (c2 != null) {
                while (c2.moveToNext()) {
                    String dStr = c2.getString(0);
                    double amt = c2.getDouble(1);
                    if (isSameMonth(dStr, targetMonth, targetYear)) {
                        mExpense += amt;
                    }
                }
                c2.close();
            }

            double netCashFlow = mIncome - mExpense;

            TextView tvInc = view.findViewById(R.id.tvDashMonthIncome);
            TextView tvExp = view.findViewById(R.id.tvDashMonthExpense);
            TextView tvNet = view.findViewById(R.id.tvDashMonthNet);

            if (tvInc != null) tvInc.setText(FormatUtils.formatCurrency(mIncome));
            if (tvExp != null) tvExp.setText(FormatUtils.formatCurrency(mExpense));
            if (tvNet != null) tvNet.setText(FormatUtils.formatCurrency(netCashFlow));
        } catch (Exception ignored) {}

        // Today's Milk
        try {
            String today = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
            Cursor cMilk = db.rawQuery("SELECT SUM(litres) FROM milk_production WHERE datetime = ?", new String[]{today});
            if (cMilk.moveToFirst() && tvStatMilk != null) {
                double total = cMilk.getDouble(0);
                tvStatMilk.setText(String.format(Locale.US, "%.1f L", total));
            }
            cMilk.close();
        } catch (Exception ignored) {}

        // Low Stock Count
        try {
            Cursor cStock = db.rawQuery("SELECT COUNT(*) FROM inventory WHERE quantity <= min_quantity AND min_quantity > 0", null);
            if (cStock.moveToFirst() && tvStatLowStock != null) {
                tvStatLowStock.setText(String.valueOf(cStock.getInt(0)));
            }
            cStock.close();
        } catch (Exception ignored) {}

        // Pending Vaccinations Count
        try {
            AlarmScheduler.updateOverdueStatus(db);
            Cursor cVac = db.rawQuery("SELECT COUNT(*) FROM vaccinations WHERE status = 'Pending' OR status = 'Overdue'", null);
            if (cVac.moveToFirst() && tvStatVaccines != null) {
                tvStatVaccines.setText(String.valueOf(cVac.getInt(0)));
            }
            cVac.close();
        } catch (Exception ignored) {}

        // Intelligence Feed
        loadFarmBrief(db);
        loadWhatChanged(db);
        loadFarmInsights(db);
    }

    private int countSpecies(SQLiteDatabase db, String species) {
        if (db == null || species == null) return 0;
        int count = 0;
        try {
            Cursor c1 = db.rawQuery("SELECT COUNT(*) FROM animas WHERE (species LIKE ? OR species LIKE ?) AND (lifecycle_status = 'Active' OR lifecycle_status IS NULL OR lifecycle_status = '')", new String[]{species, species + "s"});
            if (c1.moveToFirst()) count += c1.getInt(0);
            c1.close();

            Cursor c2 = db.rawQuery("SELECT SUM(current_quantity) FROM livestock_batches WHERE species LIKE ? AND (status = 'Active' OR status IS NULL OR status = '')", new String[]{species});
            if (c2.moveToFirst()) count += c2.getInt(0);
            c2.close();
        } catch (Exception ignored) {}
        return count;
    }

    public static boolean isSameMonth(String dateStr, int targetMonth, int targetYear) {
        if (dateStr == null || dateStr.trim().isEmpty()) return false;
        String s = dateStr.trim();
        // Try dd-MM-yyyy
        if (s.matches("^\\d{1,2}-\\d{1,2}-\\d{4}.*")) {
            String[] parts = s.split("[- ]");
            try {
                int month = Integer.parseInt(parts[1]) - 1; // 0-based
                int year = Integer.parseInt(parts[2]);
                return month == targetMonth && year == targetYear;
            } catch (Exception ignored) {}
        }
        // Try yyyy-MM-dd
        if (s.matches("^\\d{4}-\\d{1,2}-\\d{1,2}.*")) {
            String[] parts = s.split("[- ]");
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1; // 0-based
                return month == targetMonth && year == targetYear;
            } catch (Exception ignored) {}
        }
        return false;
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
            badgeTv.setTextColor(Color.WHITE);

            if (insight.getPriority() == Insight.Priority.CRITICAL) {
                badgeTv.setBackgroundColor(Color.parseColor("#C62828"));
            } else if (insight.getPriority() == Insight.Priority.ATTENTION) {
                badgeTv.setBackgroundColor(Color.parseColor("#EF6C00"));
            } else {
                badgeTv.setBackgroundColor(Color.parseColor("#2E7D32"));
            }
            headerLayout.addView(badgeTv);

            TextView titleTv = new TextView(getActivity());
            titleTv.setText(" " + insight.getTitle());
            titleTv.setTextSize(12);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            titleTv.setTextColor(Color.parseColor("#1B5E20"));
            headerLayout.addView(titleTv);

            itemLayout.addView(headerLayout);

            TextView msgTv = new TextView(getActivity());
            msgTv.setText(insight.getMessage());
            msgTv.setTextSize(11);
            msgTv.setTextColor(Color.parseColor("#424242"));
            msgTv.setPadding(0, 2, 0, 0);
            itemLayout.addView(msgTv);

            layoutInsightsContent.addView(itemLayout);
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
}
