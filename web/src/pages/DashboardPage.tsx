import React from 'react';
import { Farm, FinancialItem, LivestockItem, CropItem, InventoryItem } from '../types';
import {
  TrendingUp,
  TrendingDown,
  Beef,
  Sprout,
  AlertTriangle,
  Receipt,
  Layers
} from 'lucide-react';

interface DashboardPageProps {
  farm: Farm;
  financials: FinancialItem[];
  livestock: LivestockItem[];
  crops: CropItem[];
  inventory: InventoryItem[];
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  farm,
  financials,
  livestock,
  crops,
  inventory
}) => {
  const totalIncome = financials
    .filter((f) => f.type === 'INCOME')
    .reduce((sum, f) => sum + f.amount, 0);

  const totalExpense = financials
    .filter((f) => f.type === 'EXPENSE')
    .reduce((sum, f) => sum + f.amount, 0);

  const netCashFlow = totalIncome - totalExpense;

  const lowStockItems = inventory.filter((i) => i.quantity <= i.reorderThreshold);

  return (
    <div className="space-y-6">
      {/* Page Title & Farm Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Farm Overview Dashboard</h1>
          <p className="text-sm text-gray-500">
            Real-time analytics and management summary for <span className="font-semibold text-emerald-800">{farm.name}</span>
          </p>
        </div>
        <div className="mt-2 sm:mt-0 text-xs bg-emerald-100 text-emerald-900 border border-emerald-300 font-semibold px-3 py-1.5 rounded-lg inline-block">
          [LIVE DATABASE]
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase">Total Income</p>
              <h3 className="text-xl font-bold text-emerald-700 mt-1">
                {farm.currency} {totalIncome.toLocaleString()}
              </h3>
            </div>
            <div className="bg-emerald-100 text-emerald-700 p-2 rounded-lg">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase">Total Expenses</p>
              <h3 className="text-xl font-bold text-rose-700 mt-1">
                {farm.currency} {totalExpense.toLocaleString()}
              </h3>
            </div>
            <div className="bg-rose-100 text-rose-700 p-2 rounded-lg">
              <TrendingDown className="w-5 h-5" />
            </div>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase">Net Cash Flow</p>
              <h3 className={`text-xl font-bold mt-1 ${netCashFlow >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>
                {farm.currency} {netCashFlow.toLocaleString()}
              </h3>
            </div>
            <div className="bg-blue-100 text-blue-700 p-2 rounded-lg">
              <Receipt className="w-5 h-5" />
            </div>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase">Livestock Population</p>
              <h3 className="text-xl font-bold text-gray-900 mt-1">
                {livestock.length} Active Animals
              </h3>
            </div>
            <div className="bg-amber-100 text-amber-700 p-2 rounded-lg">
              <Beef className="w-5 h-5" />
            </div>
          </div>
        </div>
      </div>

      {/* Main Grid Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Low Stock Alerts */}
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-gray-900 flex items-center">
              <AlertTriangle className="w-5 h-5 text-amber-600 mr-2" />
              Low Stock & Reorder Alerts
            </h3>
            <span className="text-xs bg-amber-100 text-amber-800 font-bold px-2 py-0.5 rounded">
              {lowStockItems.length} Warning
            </span>
          </div>

          {lowStockItems.length === 0 ? (
            <p className="text-sm text-gray-500">All inventory levels are within safe operating limits.</p>
          ) : (
            <div className="space-y-2">
              {lowStockItems.map((item) => (
                <div key={item.id} className="p-3 bg-amber-50 border border-amber-200 rounded-lg flex justify-between items-center text-sm">
                  <div>
                    <span className="font-semibold text-gray-900">{item.name}</span>
                    <span className="text-xs text-gray-500 ml-2">({item.category})</span>
                  </div>
                  <div className="text-right">
                    <span className="font-bold text-amber-900">{item.quantity} {item.unit}</span>
                    <span className="text-xs text-amber-700 block">Threshold: {item.reorderThreshold}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Active Crop Enterprises */}
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-gray-900 flex items-center">
              <Sprout className="w-5 h-5 text-emerald-600 mr-2" />
              Active Crop Enterprises
            </h3>
            <span className="text-xs text-gray-500">{crops.length} Fields Registered</span>
          </div>

          <div className="space-y-2">
            {crops.map((crop) => (
              <div key={crop.id} className="p-3 bg-gray-50 border border-gray-200 rounded-lg flex justify-between items-center text-sm">
                <div>
                  <span className="font-semibold text-gray-900">{crop.cropType}</span>
                  <span className="text-xs text-gray-500 block">{crop.fieldName} — {crop.areaAcres} Acres</span>
                </div>
                <div className="text-right">
                  <span className="text-xs bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded">
                    {crop.status}
                  </span>
                  <span className="text-xs text-gray-500 block mt-1">Harvest: {crop.expectedHarvest}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
