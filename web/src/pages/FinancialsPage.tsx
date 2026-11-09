import React, { useState } from 'react';
import { FinancialItem, Farm } from '../types';
import { CircleDollarSign, TrendingUp, TrendingDown, RefreshCw, Plus, X } from 'lucide-react';

interface FinancialsPageProps {
  farm: Farm;
  financials: FinancialItem[];
  onRecordEntry?: (entry_type: string, category: string, amount: number, notes: string) => void;
}

export const FinancialsPage: React.FC<FinancialsPageProps> = ({ farm, financials, onRecordEntry }) => {
  const [showModal, setShowModal] = useState(false);
  const [entryType, setEntryType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE');
  const [category, setCategory] = useState('Veterinary Treatment');
  const [amount, setAmount] = useState('');
  const [notes, setNotes] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onRecordEntry && amount) {
      onRecordEntry(entryType, category, parseFloat(amount), notes);
      setAmount('');
      setNotes('');
      setShowModal(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Financial Ledgers & Reversals</h1>
          <p className="text-sm text-gray-500">Immutable income & expense accounting with transaction audit logs (PostgreSQL Live)</p>
        </div>
        <div className="mt-2 sm:mt-0 flex space-x-2">
          <button
            onClick={() => setShowModal(true)}
            className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg flex items-center space-x-1"
          >
            <Plus className="w-4 h-4" />
            <span>Record Entry</span>
          </button>
        </div>
      </div>

      {showModal && (
        <div className="bg-emerald-50 border border-emerald-200 p-5 rounded-xl mb-4 space-y-3">
          <div className="flex justify-between items-center border-b pb-2">
            <h3 className="font-bold text-emerald-900 text-sm flex items-center">
              <CircleDollarSign className="w-4 h-4 mr-1.5 text-emerald-700" />
              Record Financial Ledger Entry (PostgreSQL)
            </h3>
            <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600">
              <X className="w-4 h-4" />
            </button>
          </div>
          <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Entry Type</label>
              <select
                value={entryType}
                onChange={(e) => setEntryType(e.target.value as 'INCOME' | 'EXPENSE')}
                className="w-full p-2 border rounded text-sm bg-white"
              >
                <option value="EXPENSE">EXPENSE (-)</option>
                <option value="INCOME">INCOME (+)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Category</label>
              <input
                type="text"
                placeholder="Category (e.g. Feed, Vet, Sales)"
                required
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Amount ({farm.currency})</label>
              <input
                type="number"
                step="0.01"
                placeholder="e.g. 150000"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Notes / Ref</label>
              <input
                type="text"
                placeholder="Notes (optional)"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div className="sm:col-span-4 flex justify-end space-x-2 pt-2">
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="bg-gray-300 hover:bg-gray-400 text-gray-800 font-medium py-1.5 px-4 rounded text-xs"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="bg-emerald-800 hover:bg-emerald-700 text-white font-semibold py-1.5 px-4 rounded text-xs"
              >
                Save Ledger Entry
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Ledger Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="p-4 bg-gray-50 border-b flex justify-between items-center text-xs text-gray-500 font-semibold">
          <span>TRANSACTION LEDGER HISTORY</span>
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
              {financials.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-gray-400">
                    No ledger entries recorded yet.
                  </td>
                </tr>
              ) : (
                financials.map((f) => (
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
                    <td className="p-3 text-gray-600 text-xs">{f.notes || '-'}</td>
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
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
