import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { DashboardPage } from './pages/DashboardPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { ProvidersPage } from './pages/ProvidersPage';
import { RoutingPage } from './pages/RoutingPage';
import { RefundsPage } from './pages/RefundsPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { PlaygroundPage } from './pages/PlaygroundPage';

export function App() {
  const [activeTab, setActiveTab] = useState('dashboard');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'dashboard' && <DashboardPage onNavigate={setActiveTab} />}
        {activeTab === 'transactions' && <TransactionsPage />}
        {activeTab === 'providers' && <ProvidersPage />}
        {activeTab === 'routing' && <RoutingPage />}
        {activeTab === 'refunds' && <RefundsPage />}
        {activeTab === 'analytics' && <AnalyticsPage />}
        {activeTab === 'playground' && <PlaygroundPage />}
      </main>

      <footer className="border-t border-slate-900 bg-slate-950 py-6 text-center text-xs text-slate-600">
        <p>PayRoute &bull; Intelligent Payment Routing &amp; Failover Engine &bull; Enterprise Simulation</p>
      </footer>
    </div>
  );
}

export default App;
