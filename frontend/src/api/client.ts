import axios from 'axios';
import {
  AnalyticsOverview,
  PaymentProvider,
  PaymentResponse,
  PaymentTransaction,
  PaymentAttempt,
  ProviderAnalytics,
  Refund,
  RoutingRule,
  TransactionVolume,
  Merchant
} from '../types';

const API_BASE = '/api';

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const api = {
  // Analytics
  getAnalyticsOverview: () => apiClient.get<AnalyticsOverview>('/analytics/overview').then(r => r.data),
  getProviderAnalytics: () => apiClient.get<ProviderAnalytics[]>('/analytics/providers').then(r => r.data),
  getTransactionVolume: (hours = 24) => apiClient.get<TransactionVolume[]>(`/analytics/transactions?hoursBack=${hours}`).then(r => r.data),
  
  // Payments
  getPayments: (page = 0, size = 20) => apiClient.get<{ content: PaymentTransaction[]; totalElements: number; totalPages: number }>(`/payments?page=${page}&size=${size}`).then(r => r.data),
  getPaymentById: (id: string) => apiClient.get<PaymentTransaction>(`/payments/${id}`).then(r => r.data),
  getPaymentAttempts: (id: string) => apiClient.get<PaymentAttempt[]>(`/payments/${id}/attempts`).then(r => r.data),
  initiatePayment: (payload: any, idempotencyKey: string, merchantCode?: string) => {
    const headers: Record<string, string> = {
      'Idempotency-Key': idempotencyKey,
    };
    if (merchantCode) {
      headers['X-Merchant-Code'] = merchantCode;
    }
    return apiClient.post<PaymentTransaction>('/payments', payload, { headers }).then(r => r.data);
  },

  // Providers
  getProviders: () => apiClient.get<PaymentProvider[]>('/providers').then(r => r.data),
  updateProvider: (id: string, payload: any) => apiClient.put<PaymentProvider>(`/providers/${id}`, payload).then(r => r.data),
  updateProviderHealth: (id: string, payload: { status?: string; circuitBreakerState?: string; resetCircuitBreaker?: boolean }) =>
    apiClient.patch<PaymentProvider>(`/providers/${id}/health`, payload).then(r => r.data),

  // Routing
  getRoutingRules: () => apiClient.get<RoutingRule[]>('/routing/rules').then(r => r.data),
  createRoutingRule: (payload: any) => apiClient.post<RoutingRule>('/routing/rules', payload).then(r => r.data),
  updateRoutingRule: (id: string, payload: any) => apiClient.put<RoutingRule>(`/routing/rules/${id}`, payload).then(r => r.data),

  // Refunds
  getRefunds: (page = 0, size = 20) => apiClient.get<{ content: Refund[]; totalElements: number; totalPages: number }>(`/refunds?page=${page}&size=${size}`).then(r => r.data),
  initiateRefund: (payload: { transactionId: string; amount: number; reason?: string }) =>
    apiClient.post<Refund>('/refunds', payload).then(r => r.data),

  // Merchants
  getMerchants: () => apiClient.get<Merchant[]>('/merchants').then(r => r.data),
  registerMerchant: (payload: any) => apiClient.post<Merchant>('/merchants', payload).then(r => r.data),
};
