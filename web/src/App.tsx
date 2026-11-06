import React, { useState } from 'react';
import { Farm } from './types';
import {
  DEMO_FARMS,
  DEMO_LIVESTOCK,
  DEMO_BATCHES,
  DEMO_CROPS,
  DEMO_FINANCIALS,
  DEMO_INVENTORY
} from './data/demoData';
import { Header } from './components/Header';
import { Sidebar, NavTab } from './components/Sidebar';
import { LoginPage } from './pages/LoginPage';
import { FarmSelectorPage } from './pages/FarmSelectorPage';
import { DashboardPage } from './pages/DashboardPage';
import { LivestockPage } from './pages/LivestockPage';
import { CropsPage } from './pages/CropsPage';
import { FinancialsPage } from './pages/FinancialsPage';
import { InventoryPage } from './pages/InventoryPage';
import { ReportsPage } from './pages/ReportsPage';
import { SettingsPage } from './pages/SettingsPage';
import { Menu } from 'lucide-react';

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(true);
  const [userEmail, setUserEmail] = useState<string>('mugarura@mfarm.dev');
  const [activeFarm, setActiveFarm] = useState<Farm | null>(DEMO_FARMS[0]);
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);

  if (!isAuthenticated) {
    return (
      <LoginPage
        onLoginSuccess={(email) => {
          setUserEmail(email);
          setIsAuthenticated(true);
        }}
      />
    );
  }

  if (!activeFarm) {
    return (
      <FarmSelectorPage
        farms={DEMO_FARMS}
        onSelectFarm={(farm) => setActiveFarm(farm)}
      />
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      {/* Top Header */}
      <Header
        currentFarm={activeFarm}
        onSwitchFarm={() => setActiveFarm(null)}
        onLogout={() => {
          setIsAuthenticated(false);
          setActiveFarm(null);
        }}
        userEmail={userEmail}
      />

      {/* Main Container */}
      <div className="flex-1 max-w-7xl w-full mx-auto flex">
        {/* Sidebar */}
        <Sidebar
          activeTab={activeTab}
          onSelectTab={(tab) => setActiveTab(tab)}
          isOpenMobile={mobileMenuOpen}
          onCloseMobile={() => setMobileMenuOpen(false)}
        />

        {/* Content Area */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 min-w-0">
          {/* Mobile Menu Toggle Button */}
          <div className="md:hidden mb-4 flex items-center justify-between bg-white p-3 rounded-xl border shadow-sm">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="flex items-center space-x-2 text-sm font-semibold text-emerald-800"
            >
              <Menu className="w-5 h-5" />
              <span>Menu</span>
            </button>
            <span className="text-xs font-bold text-gray-500 uppercase">{activeTab}</span>
          </div>

          {/* Active View Render */}
          {activeTab === 'dashboard' && (
            <DashboardPage
              farm={activeFarm}
              financials={DEMO_FINANCIALS}
              livestock={DEMO_LIVESTOCK}
              crops={DEMO_CROPS}
              inventory={DEMO_INVENTORY}
            />
          )}

          {activeTab === 'livestock' && (
            <LivestockPage livestock={DEMO_LIVESTOCK} batches={DEMO_BATCHES} />
          )}

          {activeTab === 'crops' && <CropsPage crops={DEMO_CROPS} />}

          {activeTab === 'financials' && (
            <FinancialsPage farm={activeFarm} financials={DEMO_FINANCIALS} />
          )}

          {activeTab === 'inventory' && (
            <InventoryPage inventory={DEMO_INVENTORY} />
          )}

          {activeTab === 'reports' && <ReportsPage />}

          {activeTab === 'settings' && <SettingsPage currentFarm={activeFarm} />}
        </main>
      </div>
    </div>
  );
}
