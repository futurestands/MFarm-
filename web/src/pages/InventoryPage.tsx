import React from 'react';
import { InventoryItem } from '../types';
import { Boxes, AlertTriangle, Plus } from 'lucide-react';

interface InventoryPageProps {
  inventory: InventoryItem[];
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ inventory }) => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Stock & Feed Inventory</h1>
          <p className="text-sm text-gray-500">Monitor store room balances, feeds, fertilizers, and veterinary supplies</p>
        </div>
        <button className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg flex items-center space-x-1 mt-2 sm:mt-0">
          <Plus className="w-4 h-4" />
          <span>Add Stock Item</span>
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-600 font-semibold text-xs uppercase border-b">
              <tr>
                <th className="p-3">Item Name</th>
                <th className="p-3">Category</th>
                <th className="p-3">Current Balance</th>
                <th className="p-3">Reorder Threshold</th>
                <th className="p-3">Stock Health</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {inventory.map((item) => {
                const isLow = item.quantity <= item.reorderThreshold;
                return (
                  <tr key={item.id} className="hover:bg-gray-50/50">
                    <td className="p-3 font-semibold text-gray-900 flex items-center">
                      <Boxes className="w-4 h-4 text-emerald-600 mr-2" />
                      {item.name}
                    </td>
                    <td className="p-3 text-gray-600">{item.category}</td>
                    <td className="p-3 font-bold text-gray-900">
                      {item.quantity} {item.unit}
                    </td>
                    <td className="p-3 text-gray-500 text-xs">{item.reorderThreshold} {item.unit}</td>
                    <td className="p-3">
                      {isLow ? (
                        <span className="bg-amber-100 text-amber-800 text-xs font-bold px-2 py-0.5 rounded flex items-center w-max">
                          <AlertTriangle className="w-3.5 h-3.5 mr-1" /> Reorder Warning
                        </span>
                      ) : (
                        <span className="bg-emerald-100 text-emerald-800 text-xs font-bold px-2 py-0.5 rounded">
                          Optimal Stock
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
