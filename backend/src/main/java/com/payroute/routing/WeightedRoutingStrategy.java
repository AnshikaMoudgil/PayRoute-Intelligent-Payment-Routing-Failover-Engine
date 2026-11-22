package com.payroute.routing;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;

@Component
public class WeightedRoutingStrategy implements RoutingStrategy {

    private final SecureRandom random = new SecureRandom();

    @Override
    public RoutingStrategyType getStrategyType() {
        return RoutingStrategyType.WEIGHTED;
    }

    @Override
    public List<PaymentProvider> rankProviders(List<PaymentProvider> candidates,
                                               PaymentTransaction transaction,
                                               Map<String, Integer> providerWeights) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate total weight for eligible candidates
        Map<String, Integer> effectiveWeights = new HashMap<>();
        int totalWeight = 0;
        for (PaymentProvider p : candidates) {
            int weight = (providerWeights != null && providerWeights.containsKey(p.getCode()))
                    ? Math.max(1, providerWeights.get(p.getCode()))
                    : 10;
            effectiveWeights.put(p.getCode(), weight);
            totalWeight += weight;
        }

        // Weighted random draw for the top candidate
        int target = random.nextInt(totalWeight) + 1;
        int running = 0;
        PaymentProvider chosen = candidates.get(0);
        for (PaymentProvider p : candidates) {
            running += effectiveWeights.get(p.getCode());
            if (running >= target) {
                chosen = p;
                break;
            }
        }

        // Place the chosen candidate first, then sort the rest by weight descending
        List<PaymentProvider> result = new ArrayList<>();
        result.add(chosen);

        final PaymentProvider selectedPrimary = chosen;
        List<PaymentProvider> remaining = candidates.stream()
                .filter(p -> !p.getId().equals(selectedPrimary.getId()))
                .sorted(Comparator.comparingInt((PaymentProvider p) -> effectiveWeights.get(p.getCode())).reversed())
                .toList();

        result.addAll(remaining);
        return result;
    }
}
