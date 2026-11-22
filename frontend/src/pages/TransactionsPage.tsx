import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { PaymentAttempt, PaymentTransaction, TransactionStatus } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import {
  ArrowLeftRight,
  Clock,
  Eye,
  Filter,
  RefreshCw,
  Search,
  ShieldCheck,
  X,
  Zap,
} from 'lucide-react';

export const TransactionsPage: React.FC = () => {
  const [transactions, setTransactions] = useState<PaymentTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTx, setSelectedTx] = useState<PaymentTransaction | null>(null);
  const [txAttempts, setTxAttempts] = useState<PaymentAttempt[]>([]);
  const [loadingAttempts, setLoadingAttempts] = useState(false);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const res = await api.getPayments(0, 50);
      setTransactions(res.content || []);
    } catch (err) {
      console.error('Failed to load transactions:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  const openTxDetails = async (tx: PaymentTransaction) => {
    setSelectedTx(tx);
    try {
      setLoadingAttempts(true);
      const attempts = await api.getPaymentAttempts(tx.id);
      setTxAttempts(attempts);
    } catch (err) {
      console.error('Failed to load attempts:', err);
    } finally {
      setLoadingAttempts(false);
    }
  };

  const filteredTransactions = transactions.filter((tx) => {
    const matchesStatus = statusFilter === 'ALL' || tx.status === statusFilter;
    const matchesSearch =
      searchQuery === '' ||
      tx.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      tx.idempotencyKey.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (tx.customerEmail && tx.customerEmail.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (tx.merchantCode && tx.merchantCode.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesStatus && matchesSearch;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Payment Transactions</h1>
          <p className="text-sm text-slate-400 mt-1">
            Search, filter, and inspect detailed gateway attempts, failover events, and latency.
          </p>
        </div>
        <button
          onClick={fetchTransactions}
          className="flex items-center space-x-2 px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm border border-slate-700 transition self-start sm:self-auto"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search by Transaction ID, Idempotency-Key, Merchant, or Email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-800/80 border border-slate-700 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
          />
        </div>

        <div className="flex items-center space-x-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-800/80 border border-slate-700 rounded-lg text-sm text-slate-200 py-2 px-3 focus:outline-none focus:border-cyan-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILED">FAILED</option>
            <option value="REFUNDED">REFUNDED</option>
            <option value="PROCESSING">PROCESSING</option>
          </select>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-900/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="py-3.5 px-4 font-semibold">Transaction ID / Key</th>
                <th className="py-3.5 px-4 font-semibold">Merchant</th>
                <th className="py-3.5 px-4 font-semibold">Amount</th>
                <th className="py-3.5 px-4 font-semibold">Gateway</th>
                <th className="py-3.5 px-4 font-semibold">Routing</th>
                <th className="py-3.5 px-4 font-semibold">Attempts</th>
                <th className="py-3.5 px-4 font-semibold">Status</th>
                <th className="py-3.5 px-4 font-semibold">Date</th>
                <th className="py-3.5 px-4 font-semibold text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {filteredTransactions.length > 0 ? (
                filteredTransactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-800/50 transition">
                    <td className="py-3.5 px-4">
                      <div className="font-mono text-xs text-cyan-400 font-semibold">{tx.id.substring(0, 8)}...</div>
                      <div className="text-[11px] text-slate-500 font-mono mt-0.5 truncate max-w-xs">{tx.idempotencyKey}</div>
                    </td>
                    <td className="py-3.5 px-4 font-medium text-slate-200">
                      {tx.merchantCode}
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-white">
                      ₹{Number(tx.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs">
                      {tx.selectedProviderCode ? (
                        <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                          {tx.selectedProviderCode}
                        </span>
                      ) : (
                        <span className="text-slate-500">None</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-slate-400">
                      {tx.routingStrategyUsed || 'PRIORITY'}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className={`text-xs font-semibold px-2 py-0.5 rounded ${
                        tx.totalAttempts > 1 ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' : 'text-slate-400'
                      }`}>
                        {tx.totalAttempts} {tx.totalAttempts > 1 ? '(Failover)' : ''}
                      </span>
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={tx.status} />
                    </td>
                    <td className="py-3.5 px-4 text-xs text-slate-400">
                      {new Date(tx.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => openTxDetails(tx)}
                        className="inline-flex items-center space-x-1 px-2.5 py-1 bg-cyan-500/10 hover:bg-cyan-500/20 text-cyan-400 rounded text-xs font-medium border border-cyan-500/20 transition"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Inspect</span>
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-500 text-sm">
                    {loading ? 'Loading transactions...' : 'No transactions found matching your criteria.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Details & Attempts Modal */}
      {selectedTx && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div>
                <h3 className="text-lg font-bold text-white">Transaction Lifecycle & Failover Trace</h3>
                <p className="text-xs text-slate-400 font-mono mt-0.5">ID: {selectedTx.id}</p>
              </div>
              <button
                onClick={() => setSelectedTx(null)}
                className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 my-5 bg-slate-800/40 p-4 rounded-xl border border-slate-800 text-xs">
              <div>
                <div className="text-slate-500">Amount & Currency</div>
                <div className="text-sm font-bold text-white mt-0.5">₹{selectedTx.amount} {selectedTx.currency}</div>
              </div>
              <div>
                <div className="text-slate-500">Merchant</div>
                <div className="text-sm font-semibold text-slate-200 mt-0.5">{selectedTx.merchantCode}</div>
              </div>
              <div>
                <div className="text-slate-500">Status</div>
                <div className="mt-0.5"><StatusBadge status={selectedTx.status} /></div>
              </div>
              <div>
                <div className="text-slate-500">Fee Charged</div>
                <div className="text-sm font-semibold text-slate-200 mt-0.5">₹{selectedTx.feeCharged}</div>
              </div>
              <div>
                <div className="text-slate-500">Strategy Applied</div>
                <div className="text-sm font-semibold text-cyan-400 mt-0.5">{selectedTx.routingStrategyUsed}</div>
              </div>
              <div>
                <div className="text-slate-500">Total Attempts</div>
                <div className="text-sm font-semibold text-white mt-0.5">{selectedTx.totalAttempts}</div>
              </div>
            </div>

            {selectedTx.failureReason && (
              <div className="mb-5 p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
                <span className="font-semibold">Failure Reason:</span> {selectedTx.failureReason}
              </div>
            )}

            {/* Attempts Timeline */}
            <div>
              <h4 className="text-sm font-semibold text-white mb-3">Gateway Execution Timeline</h4>
              {loadingAttempts ? (
                <div className="py-6 text-center text-xs text-slate-400">Loading attempts...</div>
              ) : txAttempts.length > 0 ? (
                <div className="space-y-3">
                  {txAttempts.map((attempt) => (
                    <div
                      key={attempt.id}
                      className={`p-3.5 rounded-xl border ${
                        attempt.status === 'SUCCESS'
                          ? 'bg-emerald-950/20 border-emerald-800/60'
                          : 'bg-rose-950/20 border-rose-800/60'
                      }`}
                    >
                      <div className="flex items-center justify-between text-xs">
                        <div className="flex items-center space-x-2">
                          <span className="font-bold text-white">Attempt #{attempt.attemptNumber}</span>
                          <span className="font-mono text-cyan-400 font-semibold">{attempt.providerCode}</span>
                          {attempt.isFailover && (
                            <span className="px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300 text-[10px] font-semibold">
                              FAILOVER
                            </span>
                          )}
                        </div>
                        <span className={`font-semibold ${attempt.status === 'SUCCESS' ? 'text-emerald-400' : 'text-rose-400'}`}>
                          {attempt.status} ({attempt.latencyMs}ms)
                        </span>
                      </div>

                      {attempt.errorMessage && (
                        <div className="mt-2 text-xs text-rose-300">
                          <span className="font-semibold text-rose-400">Error [{attempt.errorCode}]:</span> {attempt.errorMessage}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-4 text-center text-xs text-slate-500">No attempt records logged.</div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
