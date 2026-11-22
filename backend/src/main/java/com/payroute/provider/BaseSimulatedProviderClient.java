package com.payroute.provider;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.Refund;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
public abstract class BaseSimulatedProviderClient implements PaymentProviderClient {

    protected final SecureRandom random = new SecureRandom();

    @Override
    public ProviderTransactionResult processPayment(PaymentTransaction transaction, PaymentProvider providerConfig) {
        long targetLatency = providerConfig.getAvgLatencyMs() != null ? providerConfig.getAvgLatencyMs() : 150L;
        // Add +/- 25% jitter
        long jitter = (long) ((random.nextDouble() - 0.5) * 0.5 * targetLatency);
        long simulatedLatency = Math.max(20, targetLatency + jitter);

        try {
            Thread.sleep(Math.min(simulatedLatency, 1500)); // Sleep capped for testing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double failureProb = providerConfig.getFailureProbability() != null
                ? providerConfig.getFailureProbability().doubleValue()
                : (100.0 - (providerConfig.getSimulatedSuccessRate() != null ? providerConfig.getSimulatedSuccessRate().doubleValue() : 95.0));

        double roll = random.nextDouble() * 100.0;
        boolean isFailure = roll < failureProb;

        if (isFailure) {
            // Determine if retryable (70% of simulated failures are retryable gateway/network/rate errors, 30% are permanent)
            boolean isRetryable = random.nextDouble() < 0.70;
            String errorCode;
            String errorMessage;

            if (isRetryable) {
                int errorType = random.nextInt(4);
                switch (errorType) {
                    case 0 -> {
                        errorCode = "GATEWAY_TIMEOUT";
                        errorMessage = "Simulated gateway timed out waiting for upstream bank response";
                    }
                    case 1 -> {
                        errorCode = "RATE_LIMIT_EXCEEDED";
                        errorMessage = "Simulated provider API rate limit quota temporarily exceeded";
                    }
                    case 2 -> {
                        errorCode = "SERVICE_UNAVAILABLE";
                        errorMessage = "Simulated provider switch under temporary load shedding";
                    }
                    default -> {
                        errorCode = "NETWORK_ERROR";
                        errorMessage = "Simulated connection reset by peer during provider handshake";
                    }
                }
            } else {
                int errorType = random.nextInt(3);
                switch (errorType) {
                    case 0 -> {
                        errorCode = "INSUFFICIENT_FUNDS";
                        errorMessage = "Customer bank account has insufficient balance";
                    }
                    case 1 -> {
                        errorCode = "SUSPECTED_FRAUD";
                        errorMessage = "Provider risk score exceeded security threshold";
                    }
                    default -> {
                        errorCode = "INVALID_CARD_DATA";
                        errorMessage = "Card validation check failed or card expired";
                    }
                }
            }

            log.warn("[{}] Payment failed for tx={}: errorCode={}, retryable={}",
                    getProviderCode(), transaction.getId(), errorCode, isRetryable);

            String rawResponse = String.format("{\"provider\":\"%s\",\"status\":\"FAILED\",\"errorCode\":\"%s\",\"message\":\"%s\",\"latencyMs\":%d}",
                    getProviderCode(), errorCode, errorMessage, simulatedLatency);

            return ProviderTransactionResult.builder()
                    .success(false)
                    .providerTransactionId("sim_fail_" + UUID.randomUUID())
                    .latencyMs(simulatedLatency)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .retryable(isRetryable)
                    .rawResponse(rawResponse)
                    .build();
        }

        // Success case
        String providerTxId = "sim_" + getProviderCode().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 12);
        String rawResponse = String.format("{\"provider\":\"%s\",\"status\":\"SUCCESS\",\"providerTxId\":\"%s\",\"latencyMs\":%d,\"authCode\":\"AUTH_%d\"}",
                getProviderCode(), providerTxId, simulatedLatency, random.nextInt(900000) + 100000);

        log.info("[{}] Payment SUCCESS for tx={}: providerTxId={}, latency={}ms",
                getProviderCode(), transaction.getId(), providerTxId, simulatedLatency);

        return ProviderTransactionResult.builder()
                .success(true)
                .providerTransactionId(providerTxId)
                .latencyMs(simulatedLatency)
                .retryable(false)
                .rawResponse(rawResponse)
                .build();
    }

    @Override
    public ProviderTransactionResult processRefund(Refund refund, PaymentProvider providerConfig) {
        long latency = 80 + random.nextInt(70);
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String refundTxId = "ref_" + getProviderCode().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 10);
        String rawResponse = String.format("{\"provider\":\"%s\",\"refundStatus\":\"SETTLED\",\"providerRefundId\":\"%s\",\"amount\":%s}",
                getProviderCode(), refundTxId, refund.getAmount().toPlainString());

        return ProviderTransactionResult.builder()
                .success(true)
                .providerTransactionId(refundTxId)
                .latencyMs(latency)
                .rawResponse(rawResponse)
                .build();
    }
}
