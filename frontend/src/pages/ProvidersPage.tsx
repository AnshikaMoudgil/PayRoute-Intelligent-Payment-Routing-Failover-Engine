import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { PaymentProvider } from '../types';
import { CircuitBreakerBadge } from '../components/CircuitBreakerBadge';
import {
  AlertOctagon,
  Check,
  Cpu,
  Edit2,
  Power,
  RefreshCw,
  RotateCcw,
  Sliders,
  X,
  Zap,
} from 'lucide-react';

export const ProvidersPage: React.FC = () => {
  const [providers, setProviders] = useState<PaymentProvider[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [editingProvider, setEditingProvider] = useState<PaymentProvider | null>(null);

  const fetchProviders = async () => {
    try {
      setLoading(true);
      const data = await api.getProviders();
      setProviders(data);
    } catch (err) {
      console.error('Failed to load providers:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProviders();
  }, []);

  const handleToggleStatus = async (provider: PaymentProvider) => {
    const nextStatus = provider.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    try {
      setActionLoading(provider.id);
      await api.updateProviderHealth(provider.id, { status: nextStatus });
      await fetchProviders();
    } catch (err) {
      console.error('Failed to update status:', err);
    } finally {
      setActionLoading(null);
    }
  };

  const handleTripCircuit = async (provider: PaymentProvider) => {
    try {
      setActionLoading(provider.id);
      await api.updateProviderHealth(provider.id, { circuitBreakerState: 'OPEN' });
      await fetchProviders();
    } catch (err) {
      console.error('Failed to trip circuit breaker:', err);
    } finally {
      setActionLoading(null);
    }
  };

  const handleResetCircuit = async (provider: PaymentProvider) => {
    try {
      setActionLoading(provider.id);
      await api.updateProviderHealth(provider.id, { resetCircuitBreaker: true });
      await fetchProviders();
    } catch (err) {
      console.error('Failed to reset circuit breaker:', err);
    } finally {
      setActionLoading(null);
    }
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingProvider) return;
    try {
      await api.updateProvider(editingProvider.id, {
        name: editingProvider.name,
        baseFeePercentage: editingProvider.baseFeePercentage,
        baseFeeFlat: editingProvider.baseFeeFlat,
        avgLatencyMs: editingProvider.avgLatencyMs,
        simulatedSuccessRate: editingProvider.simulatedSuccessRate,
        failureProbability: editingProvider.failureProbability,
        priority: editingProvider.priority,
      });
      setEditingProvider(null);
      await fetchProviders();
    } catch (err) {
      console.error('Failed to save provider config:', err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Payment Gateway Management</h1>
          <p className="text-sm text-slate-400 mt-1">
            Configure simulated gateway latencies, fee structures, and test circuit breaker fault tolerance.
          </p>
        </div>
        <button
          onClick={fetchProviders}
          className="flex items-center space-x-2 px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm border border-slate-700 transition self-start sm:self-auto"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Provider Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {providers.map((p) => {
          const isHealthy = p.circuitBreakerState === 'CLOSED' && p.status === 'ACTIVE';
          const isTripped = p.circuitBreakerState === 'OPEN';

          return (
            <div
              key={p.id}
              className={`bg-slate-800/40 border rounded-2xl p-6 flex flex-col justify-between transition-all ${
                isTripped
                  ? 'border-rose-500/50 shadow-lg shadow-rose-950/20'
                  : 'border-slate-700/60 hover:border-slate-600'
              }`}
            >
              <div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-cyan-400 border border-slate-700">
                      <Cpu className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-bold text-white text-base">{p.name}</h3>
                      <span className="font-mono text-xs text-slate-400">{p.code}</span>
                    </div>
                  </div>
                  <CircuitBreakerBadge state={p.circuitBreakerState} />
                </div>

                {/* Status Pills */}
                <div className="flex items-center space-x-2 mt-4">
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                      p.status === 'ACTIVE'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : 'bg-slate-700/50 text-slate-400 border border-slate-600'
                    }`}
                  >
                    {p.status}
                  </span>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                    Priority #{p.priority}
                  </span>
                </div>

                {/* Attributes Grid */}
                <div className="grid grid-cols-2 gap-3 mt-5 p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs">
                  <div>
                    <span className="text-slate-500">Transaction Fee</span>
                    <div className="font-semibold text-slate-200 mt-0.5">
                      {p.baseFeePercentage}% + ₹{p.baseFeeFlat}
                    </div>
                  </div>
                  <div>
                    <span className="text-slate-500">Avg Latency</span>
                    <div className="font-semibold text-slate-200 mt-0.5">{p.avgLatencyMs} ms</div>
                  </div>
                  <div>
                    <span className="text-slate-500">Success Rate</span>
                    <div className="font-semibold text-emerald-400 mt-0.5">{p.simulatedSuccessRate}%</div>
                  </div>
                  <div>
                    <span className="text-slate-500">Failure Prob</span>
                    <div className="font-semibold text-rose-400 mt-0.5">{p.failureProbability}%</div>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="mt-6 pt-5 border-t border-slate-800 space-y-2">
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => handleToggleStatus(p)}
                    disabled={actionLoading === p.id}
                    className="flex items-center justify-center space-x-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium border border-slate-700 transition"
                  >
                    <Power className="w-3.5 h-3.5 text-slate-400" />
                    <span>{p.status === 'ACTIVE' ? 'Disable' : 'Enable'}</span>
                  </button>

                  <button
                    onClick={() => setEditingProvider(p)}
                    className="flex items-center justify-center space-x-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium border border-slate-700 transition"
                  >
                    <Sliders className="w-3.5 h-3.5 text-slate-400" />
                    <span>Configure</span>
                  </button>
                </div>

                {/* Circuit Breaker Simulation Controls */}
                <div className="pt-2">
                  {p.circuitBreakerState === 'OPEN' ? (
                    <button
                      onClick={() => handleResetCircuit(p)}
                      disabled={actionLoading === p.id}
                      className="w-full flex items-center justify-center space-x-1.5 px-3 py-2 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 rounded-lg text-xs font-medium border border-emerald-500/30 transition"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      <span>Reset Circuit Breaker (Force Recover)</span>
                    </button>
                  ) : (
                    <button
                      onClick={() => handleTripCircuit(p)}
                      disabled={actionLoading === p.id}
                      className="w-full flex items-center justify-center space-x-1.5 px-3 py-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 rounded-lg text-xs font-medium border border-rose-500/30 transition"
                    >
                      <AlertOctagon className="w-3.5 h-3.5" />
                      <span>Trip Circuit Breaker (Simulate Outage)</span>
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Edit Provider Modal */}
      {editingProvider && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-md w-full p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h3 className="text-lg font-bold text-white">Configure {editingProvider.name}</h3>
              <button onClick={() => setEditingProvider(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveEdit} className="space-y-4 my-5 text-sm">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Gateway Name</label>
                <input
                  type="text"
                  value={editingProvider.name}
                  onChange={(e) => setEditingProvider({ ...editingProvider, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Base Fee (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={editingProvider.baseFeePercentage}
                    onChange={(e) => setEditingProvider({ ...editingProvider, baseFeePercentage: parseFloat(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Flat Fee (₹)</label>
                  <input
                    type="number"
                    step="0.5"
                    value={editingProvider.baseFeeFlat}
                    onChange={(e) => setEditingProvider({ ...editingProvider, baseFeeFlat: parseFloat(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Avg Latency (ms)</label>
                  <input
                    type="number"
                    value={editingProvider.avgLatencyMs}
                    onChange={(e) => setEditingProvider({ ...editingProvider, avgLatencyMs: parseInt(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Priority (1 = high)</label>
                  <input
                    type="number"
                    value={editingProvider.priority}
                    onChange={(e) => setEditingProvider({ ...editingProvider, priority: parseInt(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Success Rate (%)</label>
                  <input
                    type="number"
                    step="0.5"
                    value={editingProvider.simulatedSuccessRate}
                    onChange={(e) => setEditingProvider({ ...editingProvider, simulatedSuccessRate: parseFloat(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Failure Prob (%)</label>
                  <input
                    type="number"
                    step="0.5"
                    value={editingProvider.failureProbability}
                    onChange={(e) => setEditingProvider({ ...editingProvider, failureProbability: parseFloat(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setEditingProvider(null)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold rounded-lg text-xs"
                >
                  Save Configuration
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
