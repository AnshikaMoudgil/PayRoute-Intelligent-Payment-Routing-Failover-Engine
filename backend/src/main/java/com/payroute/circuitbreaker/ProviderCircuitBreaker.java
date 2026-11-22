package com.payroute.circuitbreaker;

import com.payroute.entity.CircuitBreakerState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
public class ProviderCircuitBreaker {

    private final String providerCode;
    private final int failureThreshold;
    private final long openStateTimeoutMs;
    private final int halfOpenPermits;

    private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger halfOpenTrialCalls = new AtomicInteger(0);
    private volatile long lastStateChangedTimestamp = System.currentTimeMillis();

    public ProviderCircuitBreaker(String providerCode) {
        this(providerCode, 3, 15000L, 2);
    }

    public ProviderCircuitBreaker(String providerCode, int failureThreshold, long openStateTimeoutMs, int halfOpenPermits) {
        this.providerCode = providerCode;
        this.failureThreshold = failureThreshold;
        this.openStateTimeoutMs = openStateTimeoutMs;
        this.halfOpenPermits = halfOpenPermits;
    }

    public CircuitBreakerState getState() {
        checkAutoTransitionFromOpen();
        return state.get();
    }

    public boolean allowExecution() {
        checkAutoTransitionFromOpen();
        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED) {
            return true;
        }

        if (currentState == CircuitBreakerState.HALF_OPEN) {
            int trial = halfOpenTrialCalls.incrementAndGet();
            if (trial <= halfOpenPermits) {
                log.info("[CircuitBreaker-{}] Permitting trial execution #{} in HALF_OPEN state", providerCode, trial);
                return true;
            } else {
                log.warn("[CircuitBreaker-{}] HALF_OPEN trial capacity ({}) exhausted, blocking request", providerCode, halfOpenPermits);
                return false;
            }
        }

        // OPEN state
        return false;
    }

    public void recordSuccess() {
        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            log.info("[CircuitBreaker-{}] Trial request succeeded! Provider has recovered. Transitioning HALF_OPEN -> CLOSED", providerCode);
            state.set(CircuitBreakerState.CLOSED);
            consecutiveFailures.set(0);
            halfOpenTrialCalls.set(0);
            lastStateChangedTimestamp = System.currentTimeMillis();
        } else if (currentState == CircuitBreakerState.CLOSED) {
            consecutiveFailures.set(0);
        }
    }

    public void recordFailure() {
        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            log.warn("[CircuitBreaker-{}] Trial request failed in HALF_OPEN! Transitioning back to OPEN", providerCode);
            state.set(CircuitBreakerState.OPEN);
            lastStateChangedTimestamp = System.currentTimeMillis();
            halfOpenTrialCalls.set(0);
        } else if (currentState == CircuitBreakerState.CLOSED) {
            int currentFailures = consecutiveFailures.incrementAndGet();
            log.warn("[CircuitBreaker-{}] Recorded failure #{} (threshold: {})", providerCode, currentFailures, failureThreshold);
            if (currentFailures >= failureThreshold) {
                log.error("[CircuitBreaker-{}] Failure threshold reached! Transitioning CLOSED -> OPEN for {}ms", providerCode, openStateTimeoutMs);
                state.set(CircuitBreakerState.OPEN);
                lastStateChangedTimestamp = System.currentTimeMillis();
            }
        }
    }

    public void manualTrip() {
        log.warn("[CircuitBreaker-{}] Operator manually forced OPEN state", providerCode);
        state.set(CircuitBreakerState.OPEN);
        consecutiveFailures.set(failureThreshold);
        lastStateChangedTimestamp = System.currentTimeMillis();
    }

    public void manualReset() {
        log.info("[CircuitBreaker-{}] Operator manually reset to CLOSED state", providerCode);
        state.set(CircuitBreakerState.CLOSED);
        consecutiveFailures.set(0);
        halfOpenTrialCalls.set(0);
        lastStateChangedTimestamp = System.currentTimeMillis();
    }

    private void checkAutoTransitionFromOpen() {
        if (state.get() == CircuitBreakerState.OPEN) {
            long elapsed = System.currentTimeMillis() - lastStateChangedTimestamp;
            if (elapsed >= openStateTimeoutMs) {
                if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                    log.info("[CircuitBreaker-{}] Cooldown timeout ({}ms) expired. Transitioning OPEN -> HALF_OPEN to test provider health", providerCode, elapsed);
                    halfOpenTrialCalls.set(0);
                    lastStateChangedTimestamp = System.currentTimeMillis();
                }
            }
        }
    }

    public int getConsecutiveFailuresCount() {
        return consecutiveFailures.get();
    }
}
