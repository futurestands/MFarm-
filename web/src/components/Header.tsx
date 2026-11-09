import React from 'react';
import { Farm } from '../types';
import { Sprout, Building2, UserCircle2, Bell, LogOut } from 'lucide-react';

interface HeaderProps {
  currentFarm: Farm | null;
  onSwitchFarm: () => void;
  onLogout: () => void;
  userEmail: string;
}

export const Header: React.FC<HeaderProps> = ({
  currentFarm,
  onSwitchFarm,
  onLogout,
  userEmail
}) => {
  return (
    <header className="bg-emerald-900 text-white shadow-md sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="bg-emerald-500 p-2 rounded-lg text-emerald-950 font-bold">
            <Sprout className="w-6 h-6" />
          </div>
          <div>
            <span className="font-bold text-xl tracking-tight">MFarm</span>
          </div>
        </div>

        {/* Current Active Farm Switcher */}
        {currentFarm && (
          <button
            onClick={onSwitchFarm}
            className="flex items-center space-x-2 bg-emerald-800 hover:bg-emerald-700 px-3 py-1.5 rounded-lg text-sm transition border border-emerald-700"
          >
            <Building2 className="w-4 h-4 text-emerald-300" />
            <span className="font-medium hidden sm:inline">{currentFarm.name}</span>
            <span className="text-xs bg-emerald-950 px-1.5 py-0.5 rounded text-emerald-300 uppercase font-bold">
              {currentFarm.role}
            </span>
          </button>
        )}

        {/* User Info & Actions */}
        <div className="flex items-center space-x-4">
          <div className="hidden md:flex items-center space-x-2 text-xs text-emerald-200">
            <UserCircle2 className="w-4 h-4" />
            <span>{userEmail}</span>
          </div>

          <button
            onClick={onLogout}
            title="Log out"
            className="p-1.5 hover:bg-emerald-800 rounded-lg text-emerald-200 hover:text-white transition"
          >
            <LogOut className="w-5 h-5" />
          </button>
        </div>
      </div>
    </header>
  );
};
