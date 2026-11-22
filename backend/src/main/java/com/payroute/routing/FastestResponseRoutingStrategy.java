package com.payroute.routing;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class FastestResponseRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingStrategyType getStrategyType() {
        return RoutingStrategyType.FASTEST_RESPONSE;
    }

    @Override
    public List<PaymentProvider> rankProviders(List<PaymentProvider> candidates,
                                               PaymentTransaction transaction,
                                               Map<String, Integer> providerWeights) {
        List<PaymentProvider> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingLong(p -> p.getAvgLatencyMs() != null ? p.getAvgLatencyMs() : 9999L));
        return sorted;
    }
}
