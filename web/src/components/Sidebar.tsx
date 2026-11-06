import React from 'react';
import {
  LayoutDashboard,
  Beef,
  Sprout,
  CircleDollarSign,
  Boxes,
  FileSpreadsheet,
  Settings,
  X
} from 'lucide-react';

export type NavTab =
  | 'dashboard'
  | 'livestock'
  | 'crops'
  | 'financials'
  | 'inventory'
  | 'reports'
  | 'settings';

interface SidebarProps {
  activeTab: NavTab;
  onSelectTab: (tab: NavTab) => void;
  isOpenMobile: boolean;
  onCloseMobile: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onSelectTab,
  isOpenMobile,
  onCloseMobile
}) => {
  const navItems: { id: NavTab; label: string; icon: React.ReactNode }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
    { id: 'livestock', label: 'Livestock & Batches', icon: <Beef className="w-5 h-5" /> },
    { id: 'crops', label: 'Crop Enterprises', icon: <Sprout className="w-5 h-5" /> },
    { id: 'financials', label: 'Financial Ledgers', icon: <CircleDollarSign className="w-5 h-5" /> },
    { id: 'inventory', label: 'Stock & Feed', icon: <Boxes className="w-5 h-5" /> },
    { id: 'reports', label: 'Reports & Export', icon: <FileSpreadsheet className="w-5 h-5" /> },
    { id: 'settings', label: 'Farm Settings & Roles', icon: <Settings className="w-5 h-5" /> },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpenMobile && (
        <div
          onClick={onCloseMobile}
          className="fixed inset-0 bg-gray-900/50 z-40 md:hidden"
        />
      )}

      {/* Sidebar Panel */}
      <aside
        className={`fixed md:sticky top-16 left-0 z-40 w-64 h-[calc(100vh-4rem)] bg-white border-r border-gray-200 transition-transform duration-200 ease-in-out ${
          isOpenMobile ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
        }`}
      >
        <div className="p-4 flex justify-between items-center md:hidden border-b border-gray-100">
          <span className="font-semibold text-gray-700">Navigation Menu</span>
          <button onClick={onCloseMobile} className="p-1 text-gray-500">
            <X className="w-5 h-5" />
          </button>
        </div>

        <nav className="p-3 space-y-1">
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  onSelectTab(item.id);
                  onCloseMobile();
                }}
                className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                  isActive
                    ? 'bg-emerald-50 text-emerald-800 font-semibold'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`}
              >
                <span className={isActive ? 'text-emerald-700' : 'text-gray-400'}>
                  {item.icon}
                </span>
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="absolute bottom-4 left-4 right-4 p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-800">
          <p className="font-bold mb-1">Demonstration Mode</p>
          <p>All records presented on this interface are synthetic test samples.</p>
        </div>
      </aside>
    </>
  );
};
