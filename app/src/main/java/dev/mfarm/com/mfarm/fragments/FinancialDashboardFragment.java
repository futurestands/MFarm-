package dev.mfarm.com.mfarm.fragments;

import android.content.Intent;
import android.database.Cursor;
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

    private void loadCurrency() {
        currencySymbol = "UGX ";
        Cursor cursor = MainActivity.database.rawQuery("SELECT currency_symbol FROM farm_profile LIMIT 1", null);
        if (cursor.moveToFirst()) {
            String sym = cursor.getString(0);
            if (sym != null && !sym.isEmpty() && !"$".equals(sym)) {
                currencySymbol = sym + (sym.endsWith(" ") ? "" : " ");
            }
        }
        cursor.close();
    }

    private void loadTotals() {
        double totalIncome = 0;
        double totalExpense = 0;

        java.util.Calendar calToday = java.util.Calendar.getInstance();
        int targetMonth = calToday.get(java.util.Calendar.MONTH);
        int targetYear = calToday.get(java.util.Calendar.YEAR);

        Cursor c1 = MainActivity.database.rawQuery("SELECT date, amount FROM income", null);
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

        Cursor c2 = MainActivity.database.rawQuery("SELECT date, amount FROM expenses", null);
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

        Cursor c1 = MainActivity.database.rawQuery("SELECT date, category, amount, description FROM income ORDER BY id DESC LIMIT 10", null);
        if (c1.moveToFirst()) {
            do {
                transactionList.add("[INCOME] " + c1.getString(0) + " - " + c1.getString(1) + ": " + currencySymbol + c1.getDouble(2) + " (" + c1.getString(3) + ")");
            } while (c1.moveToNext());
        }
        c1.close();

        Cursor c2 = MainActivity.database.rawQuery("SELECT date, category, amount, description FROM expenses ORDER BY id DESC LIMIT 10", null);
        if (c2.moveToFirst()) {
            do {
                transactionList.add("[EXPENSE] " + c2.getString(0) + " - " + c2.getString(1) + ": " + currencySymbol + c2.getDouble(2) + " (" + c2.getString(3) + ")");
            } while (c2.moveToNext());
        }
        c2.close();

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
