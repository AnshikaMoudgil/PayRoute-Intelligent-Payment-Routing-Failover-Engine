import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Merchant, RoutingRule, RoutingStrategyType } from '../types';
import {
  Check,
  ChevronRight,
  Layers,
  Plus,
  RefreshCw,
  Sliders,
  Sparkles,
  X,
} from 'lucide-react';

export const RoutingPage: React.FC = () => {
  const [rules, setRules] = useState<RoutingRule[]>([]);
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // New Rule Form State
  const [newMerchantId, setNewMerchantId] = useState<string>('');
  const [newStrategy, setNewStrategy] = useState<RoutingStrategyType>('PRIORITY');
  const [weightA, setWeightA] = useState(50);
  const [weightB, setWeightB] = useState(30);
  const [weightC, setWeightC] = useState(20);
  const [minAmount, setMinAmount] = useState('0');
  const [maxAmount, setMaxAmount] = useState('10000000');

  const fetchRulesAndMerchants = async () => {
    try {
      setLoading(true);
      const [r, m] = await Promise.all([api.getRoutingRules(), api.getMerchants()]);
      setRules(r);
      setMerchants(m);
    } catch (err) {
      console.error('Failed to load rules:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRulesAndMerchants();
  }, []);

  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload: any = {
        merchantId: newMerchantId ? newMerchantId : null,
        strategy: newStrategy,
        minAmount: parseFloat(minAmount) || 0,
        maxAmount: parseFloat(maxAmount) || 10000000,
        isActive: true,
      };

      if (newStrategy === 'WEIGHTED') {
        payload.providerWeights = {
          PROVIDER_A: weightA,
          PROVIDER_B: weightB,
          PROVIDER_C: weightC,
        };
      }

      await api.createRoutingRule(payload);
      setShowCreateModal(false);
      await fetchRulesAndMerchants();
    } catch (err) {
      console.error('Failed to create routing rule:', err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Intelligent Routing Configuration</h1>
          <p className="text-sm text-slate-400 mt-1">
            Define global and merchant-specific gateway selection strategies, cost thresholds, and weighted splits.
          </p>
        </div>
        <div className="flex space-x-3">
          <button
            onClick={fetchRulesAndMerchants}
            className="flex items-center space-x-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center space-x-2 px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-medium rounded-lg text-sm transition shadow-lg shadow-cyan-500/20"
          >
            <Plus className="w-4 h-4" />
            <span>New Routing Rule</span>
          </button>
        </div>
      </div>

      {/* Strategy Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
        {[
          { name: 'PRIORITY', label: 'Priority Routing', desc: 'Routes to highest priority healthy gateway first.' },
          { name: 'LOWEST_COST', label: 'Lowest Cost', desc: 'Computes real-time gateway fee % + flat fee per transaction.' },
          { name: 'HIGHEST_SUCCESS_RATE', label: 'Success Rate', desc: 'Dynamically routes to historically highest performing gateway.' },
          { name: 'FASTEST_RESPONSE', label: 'Fastest Latency', desc: 'Directs traffic to gateway with lowest average round-trip latency.' },
          { name: 'WEIGHTED', label: 'Weighted Split', desc: 'Probabilistic traffic sharding across gateways by custom weights.' },
        ].map((s) => (
          <div key={s.name} className="bg-slate-800/30 border border-slate-800 rounded-xl p-3.5 text-xs">
            <span className="font-bold text-cyan-400 font-mono">{s.name}</span>
            <div className="font-medium text-slate-200 mt-1">{s.label}</div>
            <p className="text-slate-400 mt-1 text-[11px] leading-relaxed">{s.desc}</p>
          </div>
        ))}
      </div>

      {/* Active Rules List */}
      <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-white">Active Routing Rules ({rules.length})</h2>
          <span className="text-xs text-slate-400">Evaluated in order: Merchant overrides take precedence over Global default</span>
        </div>

        <div className="divide-y divide-slate-800">
          {rules.length > 0 ? (
            rules.map((rule) => (
              <div key={rule.id} className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-slate-800/30 transition">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-white text-base">
                      {rule.merchantCode ? `Merchant: ${rule.merchantCode}` : 'Global Default Rule'}
                    </span>
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 font-mono">
                      {rule.strategy}
                    </span>
                    {rule.isActive && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        ACTIVE
                      </span>
                    )}
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400 mt-2">
                    <span>
                      Range: <strong className="text-slate-300">₹{rule.minAmount}</strong> to <strong className="text-slate-300">₹{rule.maxAmount}</strong>
                    </span>
                    {rule.providerWeights && Object.keys(rule.providerWeights).length > 0 && (
                      <div className="flex items-center space-x-2 font-mono text-[11px]">
                        <span>Weights:</span>
                        {Object.entries(rule.providerWeights).map(([prov, w]) => (
                          <span key={prov} className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                            {prov}: {w}%
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <span className="text-xs text-slate-500">
                    Updated {new Date(rule.updatedAt || rule.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </div>
            ))
          ) : (
            <div className="py-12 text-center text-slate-500 text-sm">
              No routing rules configured.
            </div>
          )}
        </div>
      </div>

      {/* Create Rule Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-lg w-full p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h3 className="text-lg font-bold text-white">Create New Routing Rule</h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateRule} className="space-y-4 my-5 text-sm">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Scope / Target Merchant</label>
                <select
                  value={newMerchantId}
                  onChange={(e) => setNewMerchantId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                >
                  <option value="">Global Default (All Merchants)</option>
                  {merchants.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.merchantCode} &ndash; {m.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Routing Strategy</label>
                <select
                  value={newStrategy}
                  onChange={(e) => setNewStrategy(e.target.value as RoutingStrategyType)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500 font-mono"
                >
                  <option value="PRIORITY">PRIORITY &ndash; Highest Priority Gateway First</option>
                  <option value="LOWEST_COST">LOWEST_COST &ndash; Dynamic Fee Minimization</option>
                  <option value="HIGHEST_SUCCESS_RATE">HIGHEST_SUCCESS_RATE &ndash; Highest Telemetry Success</option>
                  <option value="FASTEST_RESPONSE">FASTEST_RESPONSE &ndash; Lowest Latency Gateway</option>
                  <option value="WEIGHTED">WEIGHTED &ndash; Percentage Traffic Sharding</option>
                </select>
              </div>

              {newStrategy === 'WEIGHTED' && (
                <div className="p-4 bg-slate-800/60 rounded-xl border border-slate-700 space-y-3">
                  <div className="text-xs font-semibold text-cyan-400">Configure Gateway Weights (%)</div>
                  <div>
                    <div className="flex justify-between text-xs text-slate-300 mb-1">
                      <span>PROVIDER_A (ApexPay): {weightA}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={weightA}
                      onChange={(e) => setWeightA(parseInt(e.target.value))}
                      className="w-full accent-cyan-500"
                    />
                  </div>
                  <div>
                    <div className="flex justify-between text-xs text-slate-300 mb-1">
                      <span>PROVIDER_B (BoltGateway): {weightB}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={weightB}
                      onChange={(e) => setWeightB(parseInt(e.target.value))}
                      className="w-full accent-cyan-500"
                    />
                  </div>
                  <div>
                    <div className="flex justify-between text-xs text-slate-300 mb-1">
                      <span>PROVIDER_C (CloudSettle): {weightC}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={weightC}
                      onChange={(e) => setWeightC(parseInt(e.target.value))}
                      className="w-full accent-cyan-500"
                    />
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Min Amount (₹)</label>
                  <input
                    type="number"
                    value={minAmount}
                    onChange={(e) => setMinAmount(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Max Amount (₹)</label>
                  <input
                    type="number"
                    value={maxAmount}
                    onChange={(e) => setMaxAmount(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold rounded-lg text-xs"
                >
                  Save Routing Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
