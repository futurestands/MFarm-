package dev.mfarm.com.mfarm.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.mfarm.com.mfarm.ExpenseActivity;
import dev.mfarm.com.mfarm.IncomeActivity;
import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class FinancialDashboardFragment extends Fragment {

    private TextView tvMonthIncome, tvMonthExpense, tvNetBalance;
    private Button btnRecordIncome, btnRecordExpense;
    private ListView lvFinancials;
    private List<String> transactionList = new ArrayList<>();
    private String currencySymbol = "UGX ";

    public static FinancialDashboardFragment newInstance() {
        return new FinancialDashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_financial_dashboard, container, false);

        tvMonthIncome = view.findViewById(R.id.tvMonthIncome);
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense);
        tvNetBalance = view.findViewById(R.id.tvNetBalance);
        btnRecordIncome = view.findViewById(R.id.btnRecordIncome);
        btnRecordExpense = view.findViewById(R.id.btnRecordExpense);
        lvFinancials = view.findViewById(R.id.lvFinancials);

        loadCurrency();
        loadTotals();
        loadRecentTransactions();

        btnRecordIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), IncomeActivity.class));
            }
        });

        btnRecordExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ExpenseActivity.class));
            }
        });

        return view;
    }

    private SQLiteDatabase getDb() {
        return DatabaseHelper.getDatabase(getContext() != null ? getContext() : getActivity());
    }

    private void loadCurrency() {
        currencySymbol = "UGX ";
        SQLiteDatabase db = getDb();
        if (db == null) return;
        Cursor cursor = db.rawQuery("SELECT currency_symbol FROM farm_profile LIMIT 1", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String sym = cursor.getString(0);
                if (sym != null && !sym.isEmpty() && !"$".equals(sym)) {
                    currencySymbol = sym + (sym.endsWith(" ") ? "" : " ");
                }
            }
            cursor.close();
        }
    }

    private void loadTotals() {
        double totalIncome = 0;
        double totalExpense = 0;
        SQLiteDatabase db = getDb();
        if (db == null) return;

        java.util.Calendar calToday = java.util.Calendar.getInstance();
        int targetMonth = calToday.get(java.util.Calendar.MONTH);
        int targetYear = calToday.get(java.util.Calendar.YEAR);

        Cursor c1 = db.rawQuery("SELECT date, amount FROM income", null);
        if (c1 != null) {
            while (c1.moveToNext()) {
                String dStr = c1.getString(0);
                double amt = c1.getDouble(1);
                if (dev.mfarm.com.mfarm.intelligence.FarmIntelligenceEngine.isSameMonth(dStr, targetMonth, targetYear)) {
                    totalIncome += amt;
                }
            }
            c1.close();
        }

        Cursor c2 = db.rawQuery("SELECT date, amount FROM expenses", null);
        if (c2 != null) {
            while (c2.moveToNext()) {
                String dStr = c2.getString(0);
                double amt = c2.getDouble(1);
                if (dev.mfarm.com.mfarm.intelligence.FarmIntelligenceEngine.isSameMonth(dStr, targetMonth, targetYear)) {
                    totalExpense += amt;
                }
            }
            c2.close();
        }

        double net = totalIncome - totalExpense;

        tvMonthIncome.setText(String.format(Locale.US, "%s%,.0f", currencySymbol, totalIncome));
        tvMonthExpense.setText(String.format(Locale.US, "%s%,.0f", currencySymbol, totalExpense));
        tvNetBalance.setText(String.format(Locale.US, "%s%,.0f", currencySymbol, net));
    }

    private void loadRecentTransactions() {
        transactionList.clear();
        SQLiteDatabase db = getDb();
        if (db == null) return;

        class FinancialItem {
            String type;
            String date;
            String category;
            double amount;
            String desc;
            int id;

            FinancialItem(String type, String date, String category, double amount, String desc, int id) {
                this.type = type;
                this.date = date;
                this.category = category;
                this.amount = amount;
                this.desc = desc;
                this.id = id;
            }
        }

        List<FinancialItem> items = new ArrayList<>();

        Cursor c1 = db.rawQuery("SELECT date, category, amount, description, id FROM income ORDER BY id DESC LIMIT 10", null);
        if (c1 != null) {
            while (c1.moveToNext()) {
                items.add(new FinancialItem("[INCOME]", c1.getString(0), c1.getString(1), c1.getDouble(2), c1.getString(3), c1.getInt(4)));
            }
            c1.close();
        }

        Cursor c2 = db.rawQuery("SELECT date, category, amount, description, id FROM expenses ORDER BY id DESC LIMIT 10", null);
        if (c2 != null) {
            while (c2.moveToNext()) {
                items.add(new FinancialItem("[EXPENSE]", c2.getString(0), c2.getString(1), c2.getDouble(2), c2.getString(3), c2.getInt(4)));
            }
            c2.close();
        }

        java.util.Collections.sort(items, new java.util.Comparator<FinancialItem>() {
            @Override
            public int compare(FinancialItem o1, FinancialItem o2) {
                return Integer.compare(o2.id, o1.id);
            }
        });

        int maxShow = Math.min(10, items.size());
        for (int i = 0; i < maxShow; i++) {
            FinancialItem item = items.get(i);
            String descText = (item.desc != null && !item.desc.trim().isEmpty()) ? " (" + item.desc.trim() + ")" : "";
            transactionList.add(item.type + " " + item.date + " - " + item.category + ": " + currencySymbol + String.format(Locale.US, "%,.0f", item.amount) + descText);
        }

        if (transactionList.isEmpty()) {
            transactionList.add("No financial transactions recorded yet.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, transactionList);
        lvFinancials.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTotals();
        loadRecentTransactions();
    }
}
