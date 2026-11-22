package com.payroute.routing;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class HighestSuccessRateRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingStrategyType getStrategyType() {
        return RoutingStrategyType.HIGHEST_SUCCESS_RATE;
    }

    @Override
    public List<PaymentProvider> rankProviders(List<PaymentProvider> candidates,
                                               PaymentTransaction transaction,
                                               Map<String, Integer> providerWeights) {
        List<PaymentProvider> sorted = new ArrayList<>(candidates);
        // Descending order of simulatedSuccessRate
        sorted.sort(Comparator.comparing((PaymentProvider p) -> 
                p.getSimulatedSuccessRate() != null ? p.getSimulatedSuccessRate() : BigDecimal.ZERO)
                .reversed());
        return sorted;
    }
}
