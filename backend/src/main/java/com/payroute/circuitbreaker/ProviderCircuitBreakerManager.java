package com.payroute.circuitbreaker;

import com.payroute.entity.CircuitBreakerState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderCircuitBreakerManager {

    private final Map<String, ProviderCircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    @Value("${payroute.circuit-breaker.wait-duration-in-open-state-ms:15000}")
    private long openStateTimeoutMs;

    @Value("${payroute.circuit-breaker.minimum-number-of-calls:3}")
    private int failureThreshold;

    @Value("${payroute.circuit-breaker.permitted-number-of-calls-in-half-open:2}")
    private int halfOpenPermits;

    public ProviderCircuitBreaker getCircuitBreaker(String providerCode) {
        String code = providerCode.toUpperCase();
        return circuitBreakers.computeIfAbsent(code,
                k -> new ProviderCircuitBreaker(k, failureThreshold, openStateTimeoutMs, halfOpenPermits));
    }

    public boolean isExecutionAllowed(String providerCode) {
        return getCircuitBreaker(providerCode).allowExecution();
    }

    public void recordSuccess(String providerCode) {
        getCircuitBreaker(providerCode).recordSuccess();
    }

    public void recordFailure(String providerCode) {
        getCircuitBreaker(providerCode).recordFailure();
    }

    public CircuitBreakerState getState(String providerCode) {
        return getCircuitBreaker(providerCode).getState();
    }

    public void tripCircuit(String providerCode) {
        getCircuitBreaker(providerCode).manualTrip();
    }

    public void resetCircuit(String providerCode) {
        getCircuitBreaker(providerCode).manualReset();
    }

    public int getConsecutiveFailures(String providerCode) {
        return getCircuitBreaker(providerCode).getConsecutiveFailuresCount();
    }
}
