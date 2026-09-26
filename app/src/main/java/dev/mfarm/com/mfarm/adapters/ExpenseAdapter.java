package dev.mfarm.com.mfarm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import dev.mfarm.com.mfarm.R;
import dev.mfarm.com.mfarm.models.Expense;

public class ExpenseAdapter extends ArrayAdapter<Expense> {
    private Context context;
    private List<Expense> expenseList;

    public ExpenseAdapter(Context context, List<Expense> expenseList) {
        super(context, R.layout.expense_row_layout, expenseList);
        this.context = context;
        this.expenseList = expenseList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.expense_row_layout, parent, false);
        }

        Expense expense = expenseList.get(position);

        TextView tvCategory = convertView.findViewById(R.id.tvCategory);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvDescription = convertView.findViewById(R.id.tvDescription);

        tvCategory.setText(expense.getCategory());
        tvAmount.setText(String.format("%.2f", expense.getAmount()));
        tvDate.setText(expense.getDate());
        tvDescription.setText(expense.getDescription());

        return convertView;
    }
}
