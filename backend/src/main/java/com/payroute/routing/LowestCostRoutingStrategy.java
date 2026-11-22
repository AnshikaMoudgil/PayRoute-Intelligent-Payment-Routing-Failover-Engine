package com.payroute.routing;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class LowestCostRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingStrategyType getStrategyType() {
        return RoutingStrategyType.LOWEST_COST;
    }

    @Override
    public List<PaymentProvider> rankProviders(List<PaymentProvider> candidates,
                                               PaymentTransaction transaction,
                                               Map<String, Integer> providerWeights) {
        BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
        List<PaymentProvider> sorted = new ArrayList<>(candidates);

        sorted.sort(Comparator.comparing(p -> calculateFee(p, amount)));
        return sorted;
    }

    public BigDecimal calculateFee(PaymentProvider provider, BigDecimal amount) {
        BigDecimal percentage = provider.getBaseFeePercentage() != null ? provider.getBaseFeePercentage() : BigDecimal.ZERO;
        BigDecimal flat = provider.getBaseFeeFlat() != null ? provider.getBaseFeeFlat() : BigDecimal.ZERO;

        BigDecimal percentFee = amount.multiply(percentage).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return percentFee.add(flat);
    }
}
