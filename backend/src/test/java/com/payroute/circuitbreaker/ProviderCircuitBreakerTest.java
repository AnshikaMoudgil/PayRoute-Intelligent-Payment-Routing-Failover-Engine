package com.payroute.circuitbreaker;

import com.payroute.entity.CircuitBreakerState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderCircuitBreakerTest {

    @Test
    @DisplayName("Circuit breaker transitions: CLOSED -> OPEN -> HALF_OPEN -> CLOSED")
    void testCircuitBreakerLifecycle() throws InterruptedException {
        // threshold = 2 failures, timeout = 200ms, halfOpenPermits = 1
        ProviderCircuitBreaker cb = new ProviderCircuitBreaker("TEST_GW", 2, 200L, 1);

        // 1. Initial State
        assertEquals(CircuitBreakerState.CLOSED, cb.getState());
        assertTrue(cb.allowExecution());

        // 2. First Failure
        cb.recordFailure();
        assertEquals(CircuitBreakerState.CLOSED, cb.getState());
        assertEquals(1, cb.getConsecutiveFailuresCount());
        assertTrue(cb.allowExecution());

        // 3. Second Failure -> Exceeds threshold (2) -> Trips to OPEN
        cb.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, cb.getState());
        assertFalse(cb.allowExecution(), "Requests should be rejected when OPEN");

        // 4. Wait for cool-off timeout (250ms > 200ms)
        Thread.sleep(250);

        // 5. Query state -> should have transitioned to HALF_OPEN
        assertEquals(CircuitBreakerState.HALF_OPEN, cb.getState());
        assertTrue(cb.allowExecution(), "HALF_OPEN should permit trial call");

        // 6. Trial call succeeds -> should transition back to CLOSED
        cb.recordSuccess();
        assertEquals(CircuitBreakerState.CLOSED, cb.getState());
        assertEquals(0, cb.getConsecutiveFailuresCount());
        assertTrue(cb.allowExecution());
    }

    @Test
    @DisplayName("In HALF_OPEN state, failure reverts circuit breaker back to OPEN")
    void testHalfOpenFailureRevertsToOpen() throws InterruptedException {
        ProviderCircuitBreaker cb = new ProviderCircuitBreaker("TEST_GW", 1, 100L, 1);
        cb.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, cb.getState());

        Thread.sleep(150);
        assertEquals(CircuitBreakerState.HALF_OPEN, cb.getState());

        // Trial fails
        cb.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, cb.getState());
        assertFalse(cb.allowExecution());
    }

    @Test
    @DisplayName("Manual trip and reset")
    void testManualTripAndReset() {
        ProviderCircuitBreaker cb = new ProviderCircuitBreaker("TEST_GW");
        cb.manualTrip();
        assertEquals(CircuitBreakerState.OPEN, cb.getState());
        assertFalse(cb.allowExecution());

        cb.manualReset();
        assertEquals(CircuitBreakerState.CLOSED, cb.getState());
        assertTrue(cb.allowExecution());
    }
}
