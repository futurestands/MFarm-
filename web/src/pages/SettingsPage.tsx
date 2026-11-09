import React, { useState } from 'react';
import { Farm, FarmMember, UserRole } from '../types';
import { Settings, ShieldCheck, Users, UserPlus, Shield } from 'lucide-react';

interface SettingsPageProps {
  currentFarm: Farm;
  members?: FarmMember[];
  onAddMember?: (email: string, role: UserRole) => Promise<void>;
  onUpdateRole?: (userId: string, role: UserRole) => Promise<void>;
}

export const SettingsPage: React.FC<SettingsPageProps> = ({ currentFarm, members = [], onAddMember, onUpdateRole }) => {
  const [showAddForm, setShowAddForm] = useState(false);
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<UserRole>('WORKER');
  const [statusMessage, setStatusMessage] = useState('');

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (onAddMember && email) {
      try {
        setStatusMessage('');
        await onAddMember(email, role);
        setEmail('');
        setShowAddForm(false);
        setStatusMessage(`Successfully added ${email} to ${currentFarm.name}`);
      } catch (err: any) {
        setStatusMessage(`Error: ${err.message || 'Failed to add member'}`);
      }
    }
  };

  const handleRoleChange = async (userId: string, newRole: UserRole) => {
    if (onUpdateRole) {
      try {
        setStatusMessage('');
        await onUpdateRole(userId, newRole);
        setStatusMessage('User role updated successfully');
      } catch (err: any) {
        setStatusMessage(`Error: ${err.message || 'Failed to update role'}`);
      }
    }
  };

  return (
    <div className="space-y-6">
      <div className="border-b pb-4">
        <h1 className="text-2xl font-bold text-gray-900">Settings & User Administration</h1>
        <p className="text-sm text-gray-500">Configure farm preferences, role permissions, and user team access (PostgreSQL Live)</p>
      </div>

      {statusMessage && (
        <div className={`p-3 rounded-lg text-xs font-medium ${
          statusMessage.startsWith('Error') ? 'bg-red-50 text-red-800 border border-red-200' : 'bg-emerald-50 text-emerald-800 border border-emerald-200'
        }`}>
          {statusMessage}
        </div>
      )}

      {/* Team & Role Administration Section */}
      <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b pb-3">
          <h3 className="font-bold text-gray-900 flex items-center text-base">
            <Users className="w-5 h-5 text-emerald-700 mr-2" />
            Farm Team Members & User Roles ({members.length})
          </h3>
          <button
            onClick={() => setShowAddForm(!showAddForm)}
            className="bg-emerald-800 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-1.5 rounded-lg flex items-center space-x-1 mt-2 sm:mt-0"
          >
            <UserPlus className="w-4 h-4" />
            <span>Add Team Member</span>
          </button>
        </div>

        {showAddForm && (
          <form onSubmit={handleAddMember} className="bg-emerald-50 p-4 rounded-xl border border-emerald-200 flex flex-col sm:flex-row gap-3 items-end">
            <div className="flex-1 w-full">
              <label className="block text-xs font-semibold text-gray-700 mb-1">User Email Address</label>
              <input
                type="email"
                required
                placeholder="registered_user@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full p-2 border rounded text-sm bg-white"
              />
            </div>
            <div className="w-full sm:w-40">
              <label className="block text-xs font-semibold text-gray-700 mb-1">Assign Role</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
                className="w-full p-2 border rounded text-sm bg-white"
              >
                <option value="WORKER">WORKER</option>
                <option value="MANAGER">MANAGER</option>
                <option value="OWNER">OWNER</option>
              </select>
            </div>
            <div className="flex space-x-2 w-full sm:w-auto">
              <button type="submit" className="bg-emerald-800 hover:bg-emerald-700 text-white font-semibold py-2 px-4 rounded text-xs">
                Add User
              </button>
              <button type="button" onClick={() => setShowAddForm(false)} className="bg-gray-300 text-gray-800 py-2 px-3 rounded text-xs">
                Cancel
              </button>
            </div>
          </form>
        )}

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-600 font-semibold text-xs uppercase border-b">
              <tr>
                <th className="p-3">Full Name</th>
                <th className="p-3">Email</th>
                <th className="p-3">Assigned Role</th>
                <th className="p-3 text-right">Role Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {members.length === 0 ? (
                <tr>
                  <td colSpan={4} className="p-4 text-center text-gray-400">
                    No team members loaded.
                  </td>
                </tr>
              ) : (
                members.map((m) => (
                  <tr key={m.userId} className="hover:bg-gray-50/50">
                    <td className="p-3 font-semibold text-gray-900">{m.fullName || 'User'}</td>
                    <td className="p-3 text-gray-600 text-xs">{m.email}</td>
                    <td className="p-3">
                      <span className={`inline-flex items-center text-xs font-bold px-2.5 py-0.5 rounded ${
                        m.role === 'OWNER' ? 'bg-purple-100 text-purple-900' :
                        m.role === 'MANAGER' ? 'bg-emerald-100 text-emerald-900' : 'bg-gray-100 text-gray-800'
                      }`}>
                        <Shield className="w-3 h-3 mr-1" />
                        {m.role}
                      </span>
                    </td>
                    <td className="p-3 text-right">
                      <select
                        value={m.role}
                        onChange={(e) => handleRoleChange(m.userId, e.target.value as UserRole)}
                        className="text-xs p-1 border rounded bg-white font-medium"
                      >
                        <option value="WORKER">WORKER</option>
                        <option value="MANAGER">MANAGER</option>
                        <option value="OWNER">OWNER</option>
                      </select>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
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
