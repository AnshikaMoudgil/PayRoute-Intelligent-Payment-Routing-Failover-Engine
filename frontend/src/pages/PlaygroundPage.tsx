import React, { useState } from 'react';
import { api } from '../api/client';
import { PaymentTransaction, RoutingStrategyType } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Copy,
  Cpu,
  Layers,
  Play,
  RefreshCw,
  Repeat,
  ShieldCheck,
  Sparkles,
  XCircle,
  Zap,
} from 'lucide-react';

export const PlaygroundPage: React.FC = () => {
  const [merchantCode, setMerchantCode] = useState('MERCH-ACME-001');
  const [amount, setAmount] = useState('5000');
  const [currency, setCurrency] = useState('INR');
  const [idempotencyKey, setIdempotencyKey] = useState(`idem_${Date.now()}`);
  const [lastUsedKey, setLastUsedKey] = useState<string | null>(null);
  const [customerEmail, setCustomerEmail] = useState('sarah.chen@acme.com');
  const [strategyOverride, setStrategyOverride] = useState<string>('DEFAULT');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<PaymentTransaction | null>(null);
  const [error, setError] = useState<any>(null);
  const [cachedHit, setCachedHit] = useState(false);

  const generateNewKey = () => {
    setIdempotencyKey(`idem_${Date.now()}_${Math.floor(Math.random() * 1000)}`);
  };

  const reuseLastKey = () => {
    if (lastUsedKey) {
      setIdempotencyKey(lastUsedKey);
    }
  };

  const handleExecutePayment = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResponse(null);
    setCachedHit(false);

    try {
      const payload: any = {
        amount: parseFloat(amount),
        currency,
        customerEmail,
        customerId: 'cust_8832',
      };

      if (strategyOverride !== 'DEFAULT') {
        payload.routingStrategyOverride = strategyOverride;
      }

      const res = await api.initiatePayment(payload, idempotencyKey, merchantCode);
      setResponse(res);
      setLastUsedKey(idempotencyKey);

      // If the response key matches last key and attempts already existed
      if (idempotencyKey === lastUsedKey) {
        setCachedHit(true);
      }
    } catch (err: any) {
      console.error('Execution error:', err);
      const errData = err.response?.data || { message: err.message };
      setError(errData);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">Interactive Payment Routing Playground</h1>
        <p className="text-sm text-slate-400 mt-1">
          Simulate end-to-end payment lifecycles, test duplicate idempotency locks, and observe automatic failovers.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Input Form */}
        <div className="lg:col-span-5 bg-slate-800/40 border border-slate-700/60 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800">
            <div className="flex items-center space-x-2">
              <Zap className="w-5 h-5 text-cyan-400" />
              <h2 className="text-base font-semibold text-white">Payment Request Parameters</h2>
            </div>
          </div>

          <form onSubmit={handleExecutePayment} className="space-y-4 mt-5 text-sm">
            {/* Merchant */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Merchant Profile</label>
              <select
                value={merchantCode}
                onChange={(e) => setMerchantCode(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500 font-mono text-xs"
              >
                <option value="MERCH-ACME-001">MERCH-ACME-001 (Acme Retail - Default: LOWEST_COST)</option>
                <option value="MERCH-NOVA-002">MERCH-NOVA-002 (Nova Digital - Default: HIGHEST_SUCCESS_RATE)</option>
              </select>
            </div>

            {/* Idempotency Key */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-slate-300">
                  Header: Idempotency-Key
                </label>
                <div className="flex space-x-2">
                  <button
                    type="button"
                    onClick={generateNewKey}
                    className="text-[11px] text-cyan-400 hover:text-cyan-300 flex items-center space-x-1"
                  >
                    <RefreshCw className="w-3 h-3" />
                    <span>New Key</span>
                  </button>
                  {lastUsedKey && (
                    <button
                      type="button"
                      onClick={reuseLastKey}
                      className="text-[11px] text-purple-400 hover:text-purple-300 flex items-center space-x-1"
                    >
                      <Repeat className="w-3 h-3" />
                      <span>Re-use Key</span>
                    </button>
                  )}
                </div>
              </div>
              <input
                type="text"
                required
                value={idempotencyKey}
                onChange={(e) => setIdempotencyKey(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-cyan-400 font-mono text-xs focus:outline-none focus:border-cyan-500"
              />
              <span className="text-[11px] text-slate-500 mt-1 block">
                Submitting the same key twice will return the cached response immediately.
              </span>
            </div>

            {/* Amount & Currency */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Amount</label>
                <input
                  type="number"
                  step="1"
                  min="1"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500 font-semibold"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Currency</label>
                <select
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500 font-mono"
                >
                  <option value="INR">INR (₹)</option>
                  <option value="USD">USD ($)</option>
                  <option value="EUR">EUR (€)</option>
                </select>
              </div>
            </div>

            {/* Strategy Override */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Routing Strategy Override</label>
              <select
                value={strategyOverride}
                onChange={(e) => setStrategyOverride(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500 font-mono text-xs"
              >
                <option value="DEFAULT">Default (From Merchant / Global Rules)</option>
                <option value="PRIORITY">PRIORITY (Priority Gateway First)</option>
                <option value="LOWEST_COST">LOWEST_COST (Lowest Fee Calculation)</option>
                <option value="HIGHEST_SUCCESS_RATE">HIGHEST_SUCCESS_RATE (Top Reliability)</option>
                <option value="FASTEST_RESPONSE">FASTEST_RESPONSE (Lowest Latency)</option>
                <option value="WEIGHTED">WEIGHTED (Traffic Sharding)</option>
              </select>
            </div>

            {/* Customer Email */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Customer Email</label>
              <input
                type="email"
                value={customerEmail}
                onChange={(e) => setCustomerEmail(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:border-cyan-500"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-4 flex items-center justify-center space-x-2 py-3 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold rounded-xl shadow-lg shadow-cyan-500/25 transition disabled:opacity-50"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Routing & Processing Payment...</span>
                </>
              ) : (
                <>
                  <Play className="w-4 h-4 fill-current" />
                  <span>Execute Payment Request</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Right: Real-Time Execution Trace & Response */}
        <div className="lg:col-span-7 space-y-6">
          {/* Execution Trace */}
          <div className="bg-slate-800/40 border border-slate-700/60 rounded-2xl p-6 shadow-sm">
            <h2 className="text-base font-semibold text-white mb-4 flex items-center space-x-2">
              <Layers className="w-5 h-5 text-cyan-400" />
              <span>Engine Decision & Execution Pipeline</span>
            </h2>

            {response ? (
              <div className="space-y-4">
                {/* Step 1: Idempotency */}
                <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 flex items-start space-x-3 text-xs">
                  <div className={`p-1.5 rounded-lg ${cachedHit ? 'bg-purple-500/20 text-purple-400' : 'bg-emerald-500/20 text-emerald-400'}`}>
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-white">
                      {cachedHit ? 'Idempotency Cache HIT' : 'Idempotency Lock Acquired'}
                    </div>
                    <div className="text-slate-400 mt-0.5">
                      {cachedHit
                        ? 'Key previously executed. Returned original response with zero duplicate charge.'
                        : 'Unique transaction key registered and atomically locked in database.'}
                    </div>
                  </div>
                </div>

                {/* Step 2: Routing Decision */}
                <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 flex items-start space-x-3 text-xs">
                  <div className="p-1.5 rounded-lg bg-cyan-500/20 text-cyan-400">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-white">
                      Routing Strategy Applied: <span className="text-cyan-400 font-mono">{response.routingStrategyUsed}</span>
                    </div>
                    <div className="text-slate-400 mt-0.5">
                      Primary Candidate Selected: <strong className="text-slate-200">{response.selectedProviderCode || 'None'}</strong>
                      {response.feeCharged ? ` • Fee Charged: ₹${response.feeCharged}` : ''}
                    </div>
                  </div>
                </div>

                {/* Step 3: Gateway Attempts Breakdown */}
                {response.attempts && response.attempts.length > 0 && (
                  <div className="space-y-2">
                    <span className="text-xs font-semibold text-slate-400">Gateway Execution Attempts:</span>
                    {response.attempts.map((att) => (
                      <div
                        key={att.id}
                        className={`p-3 rounded-lg border text-xs flex items-center justify-between ${
                          att.status === 'SUCCESS'
                            ? 'bg-emerald-950/20 border-emerald-800/60'
                            : 'bg-rose-950/20 border-rose-800/60'
                        }`}
                      >
                        <div className="flex items-center space-x-2">
                          <span className="font-bold text-white">Attempt #{att.attemptNumber}</span>
                          <span className="font-mono text-cyan-400">{att.providerCode}</span>
                          {att.isFailover && (
                            <span className="px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300 text-[10px] font-semibold">
                              FAILOVER
                            </span>
                          )}
                        </div>
                        <div className="flex items-center space-x-3">
                          {att.errorMessage && (
                            <span className="text-rose-300 text-[11px] truncate max-w-xs">{att.errorMessage}</span>
                          )}
                          <span className={`font-semibold ${att.status === 'SUCCESS' ? 'text-emerald-400' : 'text-rose-400'}`}>
                            {att.status} ({att.latencyMs}ms)
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {/* Step 4: Final Outcome */}
                <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-xs text-slate-400">Transaction Status</span>
                    <div className="mt-1"><StatusBadge status={response.status} /></div>
                  </div>
                  <div className="text-right">
                    <span className="text-xs text-slate-400">Amount Charged</span>
                    <div className="text-base font-bold text-white mt-0.5">
                      ₹{response.amount} {response.currency}
                    </div>
                  </div>
                </div>
              </div>
            ) : error ? (
              <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs space-y-2">
                <div className="flex items-center space-x-2 text-rose-400 font-bold text-sm">
                  <XCircle className="w-5 h-5" />
                  <span>Request Failed [{error.errorCode || error.status || 'ERROR'}]</span>
                </div>
                <p>{error.message || 'An error occurred during payment processing'}</p>
              </div>
            ) : (
              <div className="py-16 text-center text-slate-500 text-sm">
                Ready to execute. Click &ldquo;Execute Payment Request&rdquo; to simulate intelligent gateway routing, retry, and failover.
              </div>
            )}
          </div>

          {/* JSON Payload Viewer */}
          {response && (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4">
              <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-800 text-xs text-slate-400">
                <span className="font-mono">HTTP 200 OK &bull; application/json</span>
                <span className="text-cyan-400 font-mono">X-Cache-Lookup: {cachedHit ? 'HIT' : 'MISS'}</span>
              </div>
              <pre className="text-xs font-mono text-cyan-300 overflow-x-auto max-h-56 p-2">
                {JSON.stringify(response, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
