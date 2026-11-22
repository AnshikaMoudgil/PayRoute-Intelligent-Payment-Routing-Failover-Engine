package com.payroute.routing;

import com.payroute.circuitbreaker.ProviderCircuitBreakerManager;
import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.ProviderStatus;
import com.payroute.entity.RoutingStrategyType;
import com.payroute.exception.ProviderUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RoutingEngine {

    private final Map<RoutingStrategyType, RoutingStrategy> strategyMap = new ConcurrentHashMap<>();
    private final ProviderCircuitBreakerManager circuitBreakerManager;

    public RoutingEngine(List<RoutingStrategy> strategies, ProviderCircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
        for (RoutingStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyType(), strategy);
        }
    }

    /**
     * Intelligently selects and orders candidate providers based on availability, health, and strategy.
     */
    public List<PaymentProvider> resolveCandidateProviders(List<PaymentProvider> allProviders,
                                                           PaymentTransaction transaction,
                                                           RoutingStrategyType strategyType,
                                                           Map<String, Integer> providerWeights) {
        if (allProviders == null || allProviders.isEmpty()) {
            throw new ProviderUnavailableException("No payment providers configured in the system");
        }

        // 1. Filter active providers
        List<PaymentProvider> activeProviders = allProviders.stream()
                .filter(p -> p.getStatus() == ProviderStatus.ACTIVE)
                .toList();

        if (activeProviders.isEmpty()) {
            throw new ProviderUnavailableException("All payment providers are currently disabled/inactive");
        }

        // 2. Filter healthy providers according to circuit breaker state
        List<PaymentProvider> healthyCandidates = activeProviders.stream()
                .filter(p -> circuitBreakerManager.isExecutionAllowed(p.getCode()))
                .toList();

        List<PaymentProvider> finalCandidates;
        if (!healthyCandidates.isEmpty()) {
            finalCandidates = new ArrayList<>(healthyCandidates);
        } else {
            // Degraded Fallback: All circuit breakers are OPEN.
            // Pick providers with the lowest consecutive failures as best-effort attempt.
            log.warn("All provider circuit breakers are OPEN! Engaging degraded fallback across active providers.");
            finalCandidates = new ArrayList<>(activeProviders);
            finalCandidates.sort(Comparator.comparingInt(p -> circuitBreakerManager.getConsecutiveFailures(p.getCode())));
        }

        // 3. Resolve strategy
        RoutingStrategy strategy = strategyMap.getOrDefault(strategyType, strategyMap.get(RoutingStrategyType.PRIORITY));
        if (strategy == null) {
            strategy = strategyMap.values().iterator().next();
        }

        // 4. Rank candidates
        List<PaymentProvider> ranked = strategy.rankProviders(finalCandidates, transaction, providerWeights);

        log.info("RoutingEngine evaluated {} candidates using strategy {}. Top selection: {}",
                ranked.size(), strategyType, (ranked.isEmpty() ? "NONE" : ranked.get(0).getCode()));

        return ranked;
    }
}
