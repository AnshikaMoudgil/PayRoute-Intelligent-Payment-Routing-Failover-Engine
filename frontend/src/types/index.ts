export type TransactionStatus = 'CREATED' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'PENDING' | 'REFUNDED';
export type RoutingStrategyType = 'PRIORITY' | 'LOWEST_COST' | 'HIGHEST_SUCCESS_RATE' | 'FASTEST_RESPONSE' | 'WEIGHTED';
export type CircuitBreakerState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';
export type ProviderStatus = 'ACTIVE' | 'DISABLED';

export interface PaymentAttempt {
  id: string;
  providerId: string;
  providerCode: string;
  providerName: string;
  attemptNumber: number;
  status: 'SUCCESS' | 'FAILED';
  latencyMs: number;
  errorCode?: string;
  errorMessage?: string;
  isFailover: boolean;
  createdAt: string;
}

export interface PaymentTransaction {
  id: string;
  merchantId: string;
  merchantCode: string;
  merchantName: string;
  idempotencyKey: string;
  amount: number;
  currency: string;
  status: TransactionStatus;
  selectedProviderId?: string;
  selectedProviderCode?: string;
  selectedProviderName?: string;
  routingStrategyUsed: string;
  totalAttempts: number;
  feeCharged: number;
  customerId?: string;
  customerEmail?: string;
  failureReason?: string;
  attempts?: PaymentAttempt[];
  createdAt: string;
  updatedAt: string;
}

export type PaymentResponse = PaymentTransaction;

export interface PerformanceMetricsDto {
  requestsPerSecond: number;
  avgResponseTimeMs: number;
  p50ResponseTimeMs: number;
  p95ResponseTimeMs: number;
  p99ResponseTimeMs: number;
  failureRatePercentage: number;
  totalRequestsSampled: number;
}

export interface PaymentProvider {
  id: string;
  code: string;
  name: string;
  status: ProviderStatus;
  baseFeePercentage: number;
  baseFeeFlat: number;
  avgLatencyMs: number;
  simulatedSuccessRate: number;
  failureProbability: number;
  priority: number;
  circuitBreakerState: CircuitBreakerState;
  consecutiveFailures: number;
}

export interface RoutingRule {
  id: string;
  merchantId?: string;
  merchantCode?: string;
  merchantName?: string;
  strategy: RoutingStrategyType;
  providerWeights?: Record<string, number>;
  minAmount: number;
  maxAmount: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Refund {
  id: string;
  transactionId: string;
  merchantId: string;
  merchantCode: string;
  amount: number;
  reason?: string;
  status: string;
  providerRefundId?: string;
  createdAt: string;
}

export interface ProviderAnalytics {
  providerId: string;
  providerCode: string;
  providerName: string;
  status: ProviderStatus;
  circuitBreakerState: CircuitBreakerState;
  totalTransactions: number;
  successCount: number;
  failureCount: number;
  successRate: number;
  avgLatencyMs: number;
  totalFeesCollected: number;
  priority: number;
}

export interface AnalyticsOverview {
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  pendingTransactions: number;
  overallSuccessRate: number;
  totalVolume: number;
  totalProcessingFees: number;
  totalRetries: number;
  totalProviderSwitches: number;
  providerPerformance: ProviderAnalytics[];
}

export interface TransactionVolume {
  timeBucket: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
  volume: number;
}

export interface Merchant {
  id: string;
  merchantCode: string;
  name: string;
  email: string;
  rawApiKeyPrefix: string;
  status: ProviderStatus;
  defaultRoutingStrategy: RoutingStrategyType;
}
