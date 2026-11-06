import React from 'react';
import { FinancialItem, Farm } from '../types';
import { CircleDollarSign, TrendingUp, TrendingDown, RefreshCw, Plus } from 'lucide-react';

interface FinancialsPageProps {
  farm: Farm;
  financials: FinancialItem[];
}

export const FinancialsPage: React.FC<FinancialsPageProps> = ({ farm, financials }) => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Financial Ledgers & Reversals</h1>
          <p className="text-sm text-gray-500">Immutable income & expense accounting with transaction audit logs</p>
        </div>
        <div className="mt-2 sm:mt-0 flex space-x-2">
          <button className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg flex items-center space-x-1">
            <Plus className="w-4 h-4" />
            <span>Record Entry</span>
          </button>
        </div>
      </div>

      {/* Ledger Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="p-4 bg-gray-50 border-b flex justify-between items-center text-xs text-gray-500 font-semibold">
          <span>TRANSACTION LEDGER HISTORY [DEMO DATA]</span>
          <span>{financials.length} Entries Logged</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-600 font-semibold text-xs uppercase border-b">
              <tr>
                <th className="p-3">Date</th>
                <th className="p-3">Type</th>
                <th className="p-3">Category</th>
                <th className="p-3">Notes / Reference</th>
                <th className="p-3 text-right">Amount ({farm.currency})</th>
                <th className="p-3 text-center">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {financials.map((f) => (
                <tr key={f.id} className="hover:bg-gray-50/50">
                  <td className="p-3 text-gray-500 text-xs font-mono">{f.date}</td>
                  <td className="p-3">
                    <span className={`inline-flex items-center text-xs font-bold px-2 py-0.5 rounded ${
                      f.type === 'INCOME' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                    }`}>
                      {f.type === 'INCOME' ? <TrendingUp className="w-3.5 h-3.5 mr-1" /> : <TrendingDown className="w-3.5 h-3.5 mr-1" />}
                      {f.type}
                    </span>
                  </td>
                  <td className="p-3 font-semibold text-gray-800">{f.category}</td>
                  <td className="p-3 text-gray-600 text-xs">{f.notes}</td>
                  <td className={`p-3 text-right font-bold ${f.type === 'INCOME' ? 'text-emerald-700' : 'text-rose-700'}`}>
                    {f.type === 'INCOME' ? '+' : '-'}{f.amount.toLocaleString()}
                  </td>
                  <td className="p-3 text-center">
                    {f.isReversed ? (
                      <span className="text-xs bg-gray-200 text-gray-700 font-bold px-2 py-0.5 rounded flex items-center justify-center">
                        <RefreshCw className="w-3 h-3 mr-1" /> Reversed
                      </span>
                    ) : (
                      <span className="text-xs bg-emerald-50 text-emerald-700 font-semibold px-2 py-0.5 rounded">
                        Active
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
