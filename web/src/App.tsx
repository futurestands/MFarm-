import React, { useState, useEffect } from 'react';
import { Farm, LivestockItem, FinancialItem } from './types';
import { fetchFarms, fetchLivestock, fetchFinancials, createLivestock } from './services/api';
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
import { Menu, Loader2, AlertCircle } from 'lucide-react';

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [authToken, setAuthToken] = useState<string>('');
  const [userEmail, setUserEmail] = useState<string>('');
  const [farms, setFarms] = useState<Farm[]>([]);
  const [activeFarm, setActiveFarm] = useState<Farm | null>(null);
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);

  const [livestock, setLivestock] = useState<LivestockItem[]>([]);
  const [financials, setFinancials] = useState<FinancialItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>('');

  // Fetch farms upon successful authentication or token update
  useEffect(() => {
    if (isAuthenticated && authToken) {
      setIsLoading(true);
      fetchFarms(authToken)
        .then((fetchedFarms) => {
          setFarms(fetchedFarms);
          if (fetchedFarms.length > 0 && !activeFarm) {
            setActiveFarm(fetchedFarms[0]);
          }
          setIsLoading(false);
        })
        .catch((err) => {
          setErrorMessage(err.message || 'Failed to load farms from backend API');
          setIsLoading(false);
        });
    }
  }, [isAuthenticated, authToken]);

  // Fetch real database records (livestock & financials) whenever activeFarm changes
  useEffect(() => {
    if (isAuthenticated && authToken && activeFarm) {
      setIsLoading(true);
      const config = { token: authToken, farmId: activeFarm.id };
      Promise.all([
        fetchLivestock(config).catch(() => []),
        fetchFinancials(config).catch(() => [])
      ])
        .then(([fetchedLivestock, fetchedFinancials]) => {
          setLivestock(fetchedLivestock.map((item: any) => ({
            id: item.global_id,
            tag: item.tag,
            species: item.species,
            breed: item.breed || 'Unknown',
            sex: item.sex || 'UNKNOWN',
            dob: item.dob || 'N/A',
            status: item.status || 'ACTIVE'
          })));
          setFinancials(fetchedFinancials.map((item: any) => ({
            id: item.global_id,
            type: item.entry_type.toLowerCase(),
            category: item.category,
            amount: parseFloat(item.amount),
            date: item.entry_date,
            description: item.notes || ''
          })));
          setIsLoading(false);
        })
        .catch((err) => {
          setErrorMessage(err.message || 'Failed to load farm data from backend API');
          setIsLoading(false);
        });
    }
  }, [isAuthenticated, authToken, activeFarm]);

  const handleRegisterAnimal = async (tag: string, species: string, breed: string, sex: string) => {
    if (!activeFarm || !authToken) return;
    try {
      setIsLoading(true);
      await createLivestock({ token: authToken, farmId: activeFarm.id }, { tag, species, breed, sex });
      // Refresh livestock list
      const updated = await fetchLivestock({ token: authToken, farmId: activeFarm.id });
      setLivestock(updated.map((item: any) => ({
        id: item.global_id,
        tag: item.tag,
        species: item.species,
        breed: item.breed || 'Unknown',
        sex: item.sex || 'UNKNOWN',
        dob: item.dob || 'N/A',
        status: item.status || 'ACTIVE'
      })));
      setIsLoading(false);
    } catch (err: any) {
      alert(err.message || 'Failed to create animal');
      setIsLoading(false);
    }
  };

  if (!isAuthenticated) {
    return (
      <LoginPage
        onLoginSuccess={(token, email) => {
          setAuthToken(token);
          setUserEmail(email);
          setIsAuthenticated(true);
        }}
      />
    );
  }

  if (!activeFarm) {
    return (
      <FarmSelectorPage
        farms={farms}
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
          setAuthToken('');
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

          {errorMessage && (
            <div className="mb-4 bg-red-50 border border-red-200 p-4 rounded-xl flex items-center space-x-3 text-red-800">
              <AlertCircle className="w-5 h-5 flex-shrink-0 text-red-600" />
              <span className="text-sm">{errorMessage}</span>
            </div>
          )}

          {isLoading && (
            <div className="mb-4 flex items-center space-x-2 text-sm text-emerald-800 bg-emerald-50 p-3 rounded-xl">
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Synchronizing with PostgreSQL database via MFarm API...</span>
            </div>
          )}

          {/* Active View Render */}
          {activeTab === 'dashboard' && (
            <DashboardPage
              farm={activeFarm}
              financials={financials}
              livestock={livestock}
              crops={[]}
              inventory={[]}
            />
          )}

          {activeTab === 'livestock' && (
            <LivestockPage
              livestock={livestock}
              batches={[]}
              onRegisterAnimal={handleRegisterAnimal}
            />
          )}

          {activeTab === 'crops' && <CropsPage crops={[]} />}

          {activeTab === 'financials' && (
            <FinancialsPage farm={activeFarm} financials={financials} />
          )}

          {activeTab === 'inventory' && (
            <InventoryPage inventory={[]} />
          )}

          {activeTab === 'reports' && (
            <ReportsPage financials={financials} livestock={livestock} />
          )}

          {activeTab === 'settings' && <SettingsPage currentFarm={activeFarm} />}
        </main>
      </div>
    </div>
  );
}
