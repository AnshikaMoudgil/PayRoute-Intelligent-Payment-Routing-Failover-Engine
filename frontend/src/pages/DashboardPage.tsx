import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { AnalyticsOverview, PaymentTransaction, TransactionVolume } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { CircuitBreakerBadge } from '../components/CircuitBreakerBadge';
import {
  Activity,
  ArrowUpRight,
  CheckCircle2,
  Clock,
  Cpu,
  DollarSign,
  RefreshCw,
  TrendingUp,
  XCircle,
  Zap,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

export const DashboardPage: React.FC<{ onNavigate: (tab: string) => void }> = ({ onNavigate }) => {
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [volumeData, setVolumeData] = useState<TransactionVolume[]>([]);
  const [recentTransactions, setRecentTransactions] = useState<PaymentTransaction[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [ov, vol, txs] = await Promise.all([
        api.getAnalyticsOverview(),
        api.getTransactionVolume(24),
        api.getPayments(0, 7),
      ]);
      setOverview(ov);
      setVolumeData(vol);
      setRecentTransactions(txs.content || []);
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Payment Operations Overview</h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time telemetry, provider health status, and failover engine metrics.
          </p>
        </div>
        <div className="flex space-x-3">
          <button
            onClick={fetchData}
            className="flex items-center space-x-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
          <button
            onClick={() => onNavigate('playground')}
            className="flex items-center space-x-2 px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-medium rounded-lg text-sm transition shadow-lg shadow-cyan-500/20"
          >
            <Zap className="w-4 h-4" />
            <span>Simulate Payment</span>
          </button>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Volume */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Volume</span>
            <div className="p-2 bg-cyan-500/10 rounded-lg text-cyan-400">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-white">
              ₹{overview ? Number(overview.totalVolume).toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '0'}
            </div>
            <div className="flex items-center mt-2 text-xs text-emerald-400">
              <ArrowUpRight className="w-3.5 h-3.5 mr-1" />
              <span>{overview?.totalTransactions || 0} total requests</span>
            </div>
          </div>
        </div>

        {/* Card 2: Success Rate */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Success Rate</span>
            <div className="p-2 bg-emerald-500/10 rounded-lg text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-white">
              {overview ? `${overview.overallSuccessRate}%` : '0%'}
            </div>
            <div className="flex items-center mt-2 text-xs text-slate-400">
              <span className="text-emerald-400 font-medium mr-1">{overview?.successfulTransactions || 0} succeeded</span>
              <span>/ {overview?.failedTransactions || 0} failed</span>
            </div>
          </div>
        </div>

        {/* Card 3: Processing Fees */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Processing Fees</span>
            <div className="p-2 bg-blue-500/10 rounded-lg text-blue-400">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-white">
              ₹{overview ? Number(overview.totalProcessingFees).toLocaleString('en-IN', { maximumFractionDigits: 2 }) : '0.00'}
            </div>
            <div className="flex items-center mt-2 text-xs text-cyan-400">
              <span>Optimized via Lowest-Cost routing</span>
            </div>
          </div>
        </div>

        {/* Card 4: Retries & Failovers */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Failovers & Retries</span>
            <div className="p-2 bg-purple-500/10 rounded-lg text-purple-400">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-white">
              {overview?.totalProviderSwitches || 0} switches
            </div>
            <div className="flex items-center mt-2 text-xs text-slate-400">
              <span>{overview?.totalRetries || 0} retried attempts</span>
            </div>
          </div>
        </div>
      </div>

      {/* Gateway Circuit Breakers Live Banner */}
      <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <Cpu className="w-5 h-5 text-cyan-400" />
            <h2 className="text-base font-semibold text-white">Active Payment Gateways & Circuit Breakers</h2>
          </div>
          <button
            onClick={() => onNavigate('providers')}
            className="text-xs font-medium text-cyan-400 hover:text-cyan-300"
          >
            Manage Gateways &rarr;
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {overview?.providerPerformance.map((p) => (
            <div
              key={p.providerCode}
              className="bg-slate-900/80 border border-slate-800 rounded-lg p-4 flex flex-col justify-between"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-semibold text-white text-sm">{p.providerName}</h3>
                  <span className="text-xs font-mono text-slate-400">{p.providerCode}</span>
                </div>
                <CircuitBreakerBadge state={p.circuitBreakerState} />
              </div>

              <div className="grid grid-cols-3 gap-2 mt-4 pt-3 border-t border-slate-800 text-xs">
                <div>
                  <div className="text-slate-500">Success</div>
                  <div className="font-semibold text-slate-200 mt-0.5">{p.successRate}%</div>
                </div>
                <div>
                  <div className="text-slate-500">Avg Latency</div>
                  <div className="font-semibold text-slate-200 mt-0.5">{p.avgLatencyMs}ms</div>
                </div>
                <div>
                  <div className="text-slate-500">Volume</div>
                  <div className="font-semibold text-slate-200 mt-0.5">{p.totalTransactions} tx</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Volume Chart & Recent Transactions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Timeline Chart */}
        <div className="lg:col-span-2 bg-slate-800/40 border border-slate-700/60 rounded-xl p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-semibold text-white">Transaction Volume Trend</h2>
              <p className="text-xs text-slate-400">Hourly throughput distribution</p>
            </div>
          </div>
          <div className="h-64 w-full">
            {volumeData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={volumeData}>
                  <defs>
                    <linearGradient id="volumeGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#00B4D8" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#00B4D8" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1E293B" />
                  <XAxis dataKey="timeBucket" stroke="#64748B" fontSize={11} />
                  <YAxis stroke="#64748B" fontSize={11} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#0B132B', borderColor: '#334155', borderRadius: '8px' }}
                  />
                  <Area
                    type="monotone"
                    dataKey="totalCount"
                    name="Transactions"
                    stroke="#00B4D8"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#volumeGrad)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-slate-500 text-sm">
                No transaction data in this window yet. Initiate transactions via the API Playground!
              </div>
            )}
          </div>
        </div>

        {/* Recent Transactions List */}
        <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-semibold text-white">Recent Transactions</h2>
              <button
                onClick={() => onNavigate('transactions')}
                className="text-xs text-cyan-400 hover:text-cyan-300 font-medium"
              >
                View all &rarr;
              </button>
            </div>

            <div className="space-y-3">
              {recentTransactions.length > 0 ? (
                recentTransactions.map((tx) => (
                  <div
                    key={tx.id}
                    className="flex items-center justify-between p-3 bg-slate-900/60 border border-slate-800 rounded-lg text-sm"
                  >
                    <div>
                      <div className="font-medium text-slate-200">
                        ₹{Number(tx.amount).toLocaleString('en-IN')}
                      </div>
                      <div className="text-xs text-slate-400 mt-0.5">
                        {tx.merchantCode} &bull; {tx.selectedProviderCode || 'None'}
                      </div>
                    </div>
                    <div className="text-right">
                      <StatusBadge status={tx.status} />
                      <div className="text-[10px] text-slate-500 mt-1">
                        {tx.totalAttempts} {tx.totalAttempts === 1 ? 'attempt' : 'attempts'}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="py-8 text-center text-slate-500 text-sm">
                  No transactions executed yet.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
