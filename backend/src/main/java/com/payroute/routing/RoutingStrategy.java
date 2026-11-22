package com.payroute.routing;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.RoutingStrategyType;

import java.util.List;
import java.util.Map;

public interface RoutingStrategy {
    RoutingStrategyType getStrategyType();
    
    /**
     * Ranks eligible candidate providers from most suitable to least suitable.
     * 
     * @param candidates Eligible healthy active providers
     * @param transaction The payment transaction being processed
     * @param providerWeights Optional weight mapping (e.g. {"PROVIDER_A": 50, "PROVIDER_B": 30})
     * @return Ordered list of providers with the best candidate first
     */
    List<PaymentProvider> rankProviders(List<PaymentProvider> candidates,
                                         PaymentTransaction transaction,
                                         Map<String, Integer> providerWeights);
}
