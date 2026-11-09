import React, { useState } from 'react';
import { InventoryItem } from '../types';
import { Boxes, AlertTriangle, Plus, X } from 'lucide-react';

interface InventoryPageProps {
  inventory: InventoryItem[];
  onAddStockItem?: (name: string, category: string, quantity: number, unit: string, minQuantity: number) => void;
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ inventory, onAddStockItem }) => {
  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Feed');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('KG');
  const [minQuantity, setMinQuantity] = useState('10');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onAddStockItem && name) {
      onAddStockItem(name, category, parseFloat(quantity || '0'), unit, parseFloat(minQuantity || '10'));
      setName('');
      setQuantity('');
      setShowModal(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Stock & Feed Inventory</h1>
          <p className="text-sm text-gray-500">Monitor store room balances, feeds, fertilizers, and veterinary supplies (PostgreSQL Live)</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg flex items-center space-x-1 mt-2 sm:mt-0"
        >
          <Plus className="w-4 h-4" />
          <span>Add Stock Item</span>
        </button>
      </div>

      {showModal && (
        <div className="bg-emerald-50 border border-emerald-200 p-5 rounded-xl mb-4 space-y-3">
          <div className="flex justify-between items-center border-b pb-2">
            <h3 className="font-bold text-emerald-900 text-sm flex items-center">
              <Boxes className="w-4 h-4 mr-1.5 text-emerald-700" />
              Add Inventory / Feed Item (PostgreSQL)
            </h3>
            <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600">
              <X className="w-4 h-4" />
            </button>
          </div>
          <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-5 gap-3">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Item Name</label>
              <input
                type="text"
                placeholder="e.g. Dairy Meal Feed"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              >
                <option value="Feed">Feed</option>
                <option value="Medicine">Medicine / Vet</option>
                <option value="Fertilizer">Fertilizer / Seed</option>
                <option value="Equipment">Supplies / Equipment</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Quantity</label>
              <input
                type="number"
                step="0.1"
                placeholder="e.g. 500"
                required
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Unit</label>
              <input
                type="text"
                placeholder="e.g. KG, Litres, Bags"
                required
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Reorder Limit</label>
              <input
                type="number"
                step="0.1"
                placeholder="e.g. 20"
                required
                value={minQuantity}
                onChange={(e) => setMinQuantity(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div className="sm:col-span-5 flex justify-end space-x-2 pt-2">
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
                Save Stock Item
              </button>
            </div>
          </form>
        </div>
      )}

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
              {inventory.length === 0 ? (
                <tr>
                  <td colSpan={5} className="p-6 text-center text-gray-400">
                    No stock inventory items registered yet.
                  </td>
                </tr>
              ) : (
                inventory.map((item) => {
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
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
