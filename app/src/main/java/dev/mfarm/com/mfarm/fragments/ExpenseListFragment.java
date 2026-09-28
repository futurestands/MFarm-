package dev.mfarm.com.mfarm.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import dev.mfarm.com.mfarm.MainActivity;
import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.adapters.ExpenseAdapter;
import dev.mfarm.com.mfarm.models.Expense;

public class ExpenseListFragment extends Fragment {

    private List<Expense> expenseList = new ArrayList<>();
    private ExpenseAdapter adapter;

    public static ExpenseListFragment newInstance() {
        return new ExpenseListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense_list, container, false);
        ListView listView = view.findViewById(R.id.listViewExpenses);

        final android.widget.TextView emptyView = new android.widget.TextView(getActivity());
        emptyView.setText("No expenses recorded yet.");
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(20, 20, 20, 20);
        ((ViewGroup)listView.getParent()).addView(emptyView);
        listView.setEmptyView(emptyView);

        loadExpenses();

        adapter = new ExpenseAdapter(getActivity(), expenseList);
        listView.setAdapter(adapter);

        return view;
    }

    private void loadExpenses() {
        expenseList.clear();
        android.database.sqlite.SQLiteDatabase db = dev.mfarm.com.mfarm.dao.DatabaseHelper.getDatabase(getContext() != null ? getContext() : getActivity());
        if (db == null) return;

        Cursor cursor = db.rawQuery("SELECT * FROM expenses ORDER BY id DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Expense expense = new Expense(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getDouble(2),
                        cursor.getString(3),
                        cursor.getString(4)
                );
                expenseList.add(expense);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
    }
}
