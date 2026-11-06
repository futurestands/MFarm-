import React from 'react';
import { Farm } from '../types';
import { Settings, ShieldCheck, Users, Server, RefreshCw } from 'lucide-react';

interface SettingsPageProps {
  currentFarm: Farm;
}

export const SettingsPage: React.FC<SettingsPageProps> = ({ currentFarm }) => {
  return (
    <div className="space-y-6">
      <div className="border-b pb-4">
        <h1 className="text-2xl font-bold text-gray-900">Settings & User Administration</h1>
        <p className="text-sm text-gray-500">Configure farm preferences, role permissions, and cloud sync status</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Farm Settings */}
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-4">
          <h3 className="font-bold text-gray-900 flex items-center text-base">
            <Settings className="w-5 h-5 text-emerald-700 mr-2" />
            Active Farm Configuration
          </h3>
          <div className="space-y-2 text-sm text-gray-700">
            <div className="flex justify-between border-b pb-2">
              <span className="text-gray-500">Farm Name:</span>
              <span className="font-semibold">{currentFarm.name}</span>
            </div>
            <div className="flex justify-between border-b pb-2">
              <span className="text-gray-500">Location:</span>
              <span className="font-semibold">{currentFarm.location}</span>
            </div>
            <div className="flex justify-between border-b pb-2">
              <span className="text-gray-500">Operating Currency:</span>
              <span className="font-semibold">{currentFarm.currency}</span>
            </div>
            <div className="flex justify-between border-b pb-2">
              <span className="text-gray-500">Your Access Role:</span>
              <span className="font-bold text-emerald-800">{currentFarm.role}</span>
            </div>
          </div>
        </div>

        {/* Roles & Permissions */}
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-4">
          <h3 className="font-bold text-gray-900 flex items-center text-base">
            <ShieldCheck className="w-5 h-5 text-emerald-700 mr-2" />
            Role-Based Access Control (RBAC)
          </h3>
          <div className="space-y-2 text-xs text-gray-600">
            <div className="p-2.5 bg-gray-50 rounded-lg border">
              <span className="font-bold text-emerald-900 block">OWNER</span>
              <span>Full administrative control, user management, and transaction reversal rights.</span>
            </div>
            <div className="p-2.5 bg-gray-50 rounded-lg border">
              <span className="font-bold text-emerald-900 block">MANAGER</span>
              <span>Daily operational entry, livestock, crop, and inventory management.</span>
            </div>
            <div className="p-2.5 bg-gray-50 rounded-lg border">
              <span className="font-bold text-emerald-900 block">WORKER</span>
              <span>Field activity recording and basic livestock health logging.</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
