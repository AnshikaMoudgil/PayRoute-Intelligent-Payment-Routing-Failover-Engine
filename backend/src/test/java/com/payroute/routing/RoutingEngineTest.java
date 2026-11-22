package com.payroute.routing;

import com.payroute.circuitbreaker.ProviderCircuitBreakerManager;
import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.ProviderStatus;
import com.payroute.entity.RoutingStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoutingEngineTest {

    private ProviderCircuitBreakerManager circuitBreakerManager;
    private RoutingEngine routingEngine;
    private PaymentProvider providerA;
    private PaymentProvider providerB;
    private PaymentProvider providerC;

    @BeforeEach
    void setUp() {
        circuitBreakerManager = new ProviderCircuitBreakerManager();

        PriorityRoutingStrategy priorityStrategy = new PriorityRoutingStrategy();
        LowestCostRoutingStrategy costStrategy = new LowestCostRoutingStrategy();
        HighestSuccessRateRoutingStrategy successStrategy = new HighestSuccessRateRoutingStrategy();
        FastestResponseRoutingStrategy latencyStrategy = new FastestResponseRoutingStrategy();
        WeightedRoutingStrategy weightedStrategy = new WeightedRoutingStrategy();

        routingEngine = new RoutingEngine(
                List.of(priorityStrategy, costStrategy, successStrategy, latencyStrategy, weightedStrategy),
                circuitBreakerManager
        );

        providerA = PaymentProvider.builder()
                .id(UUID.randomUUID())
                .code("PROVIDER_A")
                .name("ApexPay")
                .status(ProviderStatus.ACTIVE)
                .priority(1)
                .baseFeePercentage(new BigDecimal("1.80"))
                .baseFeeFlat(new BigDecimal("2.00"))
                .avgLatencyMs(120L)
                .simulatedSuccessRate(new BigDecimal("98.00"))
                .build();

        providerB = PaymentProvider.builder()
                .id(UUID.randomUUID())
                .code("PROVIDER_B")
                .name("BoltGateway")
                .status(ProviderStatus.ACTIVE)
                .priority(2)
                .baseFeePercentage(new BigDecimal("1.20"))
                .baseFeeFlat(new BigDecimal("1.00"))
                .avgLatencyMs(190L)
                .simulatedSuccessRate(new BigDecimal("93.00"))
                .build();

        providerC = PaymentProvider.builder()
                .id(UUID.randomUUID())
                .code("PROVIDER_C")
                .name("CloudSettle")
                .status(ProviderStatus.ACTIVE)
                .priority(3)
                .baseFeePercentage(new BigDecimal("2.10"))
                .baseFeeFlat(new BigDecimal("1.50"))
                .avgLatencyMs(75L)
                .simulatedSuccessRate(new BigDecimal("96.00"))
                .build();
    }

    @Test
    @DisplayName("Priority Routing selects provider with lowest priority integer first")
    void testPriorityRouting() {
        PaymentTransaction tx = PaymentTransaction.builder().amount(new BigDecimal("5000")).build();
        List<PaymentProvider> candidates = routingEngine.resolveCandidateProviders(
                List.of(providerB, providerC, providerA), tx, RoutingStrategyType.PRIORITY, null);

        assertEquals("PROVIDER_A", candidates.get(0).getCode());
        assertEquals("PROVIDER_B", candidates.get(1).getCode());
        assertEquals("PROVIDER_C", candidates.get(2).getCode());
    }

    @Test
    @DisplayName("Lowest Cost Routing selects provider with lowest calculated transaction fee")
    void testLowestCostRouting() {
        PaymentTransaction tx = PaymentTransaction.builder().amount(new BigDecimal("5000")).build();
        // At 5000:
        // Provider A fee: 5000 * 1.8% + 2 = 90 + 2 = 92
        // Provider B fee: 5000 * 1.2% + 1 = 60 + 1 = 61 (Cheapest!)
        // Provider C fee: 5000 * 2.1% + 1.5 = 105 + 1.5 = 106.5

        List<PaymentProvider> candidates = routingEngine.resolveCandidateProviders(
                List.of(providerA, providerB, providerC), tx, RoutingStrategyType.LOWEST_COST, null);

        assertEquals("PROVIDER_B", candidates.get(0).getCode());
        assertEquals("PROVIDER_A", candidates.get(1).getCode());
        assertEquals("PROVIDER_C", candidates.get(2).getCode());
    }

    @Test
    @DisplayName("Highest Success Rate Routing selects provider with highest simulated success percentage")
    void testHighestSuccessRateRouting() {
        PaymentTransaction tx = PaymentTransaction.builder().amount(new BigDecimal("1000")).build();
        List<PaymentProvider> candidates = routingEngine.resolveCandidateProviders(
                List.of(providerB, providerA, providerC), tx, RoutingStrategyType.HIGHEST_SUCCESS_RATE, null);

        assertEquals("PROVIDER_A", candidates.get(0).getCode()); // 98%
        assertEquals("PROVIDER_C", candidates.get(1).getCode()); // 96%
        assertEquals("PROVIDER_B", candidates.get(2).getCode()); // 93%
    }

    @Test
    @DisplayName("Fastest Response Routing selects provider with lowest average latency")
    void testFastestResponseRouting() {
        PaymentTransaction tx = PaymentTransaction.builder().amount(new BigDecimal("1000")).build();
        List<PaymentProvider> candidates = routingEngine.resolveCandidateProviders(
                List.of(providerA, providerB, providerC), tx, RoutingStrategyType.FASTEST_RESPONSE, null);

        assertEquals("PROVIDER_C", candidates.get(0).getCode()); // 75ms
        assertEquals("PROVIDER_A", candidates.get(1).getCode()); // 120ms
        assertEquals("PROVIDER_B", candidates.get(2).getCode()); // 190ms
    }

    @Test
    @DisplayName("Candidate filtering excludes provider when Circuit Breaker is OPEN")
    void testCircuitBreakerExclusion() {
        // Manually trip Provider A's circuit breaker
        circuitBreakerManager.tripCircuit("PROVIDER_A");

        PaymentTransaction tx = PaymentTransaction.builder().amount(new BigDecimal("1000")).build();
        List<PaymentProvider> candidates = routingEngine.resolveCandidateProviders(
                List.of(providerA, providerB, providerC), tx, RoutingStrategyType.PRIORITY, null);

        assertEquals(2, candidates.size());
        assertEquals("PROVIDER_B", candidates.get(0).getCode());
        assertEquals("PROVIDER_C", candidates.get(1).getCode());
        assertFalse(candidates.stream().anyMatch(p -> "PROVIDER_A".equals(p.getCode())));
    }
}
