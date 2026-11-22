import React from 'react';
import { CircuitBreakerState } from '../types';
import { ShieldCheck, ShieldAlert, AlertTriangle } from 'lucide-react';

export const CircuitBreakerBadge: React.FC<{ state: CircuitBreakerState }> = ({ state }) => {
  switch (state) {
    case 'CLOSED':
      return (
        <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-md text-xs font-medium bg-emerald-950/60 text-emerald-400 border border-emerald-800">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
          <span>CLOSED (Healthy)</span>
        </span>
      );
    case 'OPEN':
      return (
        <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-md text-xs font-medium bg-rose-950/60 text-rose-400 border border-rose-800 animate-pulse">
          <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
          <span>OPEN (Tripped)</span>
        </span>
      );
    case 'HALF_OPEN':
      return (
        <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-md text-xs font-medium bg-amber-950/60 text-amber-400 border border-amber-800">
          <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
          <span>HALF_OPEN (Testing)</span>
        </span>
      );
  }
};
