import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Refund } from '../types';
import {
  AlertCircle,
  CheckCircle2,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  X,
} from 'lucide-react';

export const RefundsPage: React.FC = () => {
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Form
  const [txId, setTxId] = useState('');
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const fetchRefunds = async () => {
    try {
      setLoading(true);
      const res = await api.getRefunds(0, 50);
      setRefunds(res.content || []);
    } catch (err) {
      console.error('Failed to load refunds:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRefunds();
  }, []);

  const handleCreateRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    try {
      setSubmitting(true);
      await api.initiateRefund({
        transactionId: txId.trim(),
        amount: parseFloat(amount),
        reason: reason.trim() || undefined,
      });
      setShowModal(false);
      setTxId('');
      setAmount('');
      setReason('');
      await fetchRefunds();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to initiate refund';
      setFormError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const filteredRefunds = refunds.filter((r) => {
    return (
      searchQuery === '' ||
      r.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.transactionId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (r.merchantCode && r.merchantCode.toLowerCase().includes(searchQuery.toLowerCase()))
    );
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Refund Management</h1>
          <p className="text-sm text-slate-400 mt-1">
            Initiate full or partial refunds for settled payments and inspect gateway settlement IDs.
          </p>
        </div>
        <div className="flex space-x-3">
          <button
            onClick={fetchRefunds}
            className="flex items-center space-x-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm border border-slate-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center space-x-2 px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white font-medium rounded-lg text-sm transition shadow-lg shadow-purple-600/20"
          >
            <Plus className="w-4 h-4" />
            <span>Issue Refund</span>
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
        <input
          type="text"
          placeholder="Search by Refund ID, Original Transaction ID, or Merchant Code..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full pl-9 pr-4 py-2 bg-slate-800/80 border border-slate-700 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-purple-500"
        />
      </div>

      {/* Refunds Table */}
      <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-900/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="py-3.5 px-4 font-semibold">Refund ID</th>
                <th className="py-3.5 px-4 font-semibold">Original Transaction ID</th>
                <th className="py-3.5 px-4 font-semibold">Merchant</th>
                <th className="py-3.5 px-4 font-semibold">Refund Amount</th>
                <th className="py-3.5 px-4 font-semibold">Status</th>
                <th className="py-3.5 px-4 font-semibold">Gateway Refund ID</th>
                <th className="py-3.5 px-4 font-semibold">Reason</th>
                <th className="py-3.5 px-4 font-semibold">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {filteredRefunds.length > 0 ? (
                filteredRefunds.map((ref) => (
                  <tr key={ref.id} className="hover:bg-slate-800/50 transition">
                    <td className="py-3.5 px-4 font-mono text-xs text-purple-400 font-semibold">
                      {ref.id.substring(0, 8)}...
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs text-cyan-400">
                      {ref.transactionId.substring(0, 8)}...
                    </td>
                    <td className="py-3.5 px-4 font-medium text-slate-200">
                      {ref.merchantCode}
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-white">
                      ₹{Number(ref.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {ref.status}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-xs text-slate-400">
                      {ref.providerRefundId || 'N/A'}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-slate-400 truncate max-w-xs">
                      {ref.reason || 'N/A'}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-slate-400">
                      {new Date(ref.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500 text-sm">
                    {loading ? 'Loading refunds...' : 'No refunds recorded yet.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Issue Refund Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-md w-full p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h3 className="text-lg font-bold text-white">Issue Payment Refund</h3>
              <button onClick={() => setShowModal(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {formError && (
              <div className="mt-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleCreateRefund} className="space-y-4 my-5 text-sm">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Original Transaction UUID</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
                  value={txId}
                  onChange={(e) => setTxId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-purple-500 font-mono text-xs"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Refund Amount (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.50"
                  required
                  placeholder="500.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-purple-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Refund Reason</label>
                <input
                  type="text"
                  placeholder="Customer requested cancellation"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-purple-500"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white font-semibold rounded-lg text-xs transition"
                >
                  {submitting ? 'Processing Refund...' : 'Process Refund'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
