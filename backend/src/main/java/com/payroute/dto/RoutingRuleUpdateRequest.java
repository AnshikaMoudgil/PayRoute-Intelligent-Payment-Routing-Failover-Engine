package com.payroute.dto;

import com.payroute.entity.RoutingStrategyType;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRuleUpdateRequest {

    private RoutingStrategyType strategy;

    private Map<String, Integer> providerWeights;

    @DecimalMin("0.0")
    private BigDecimal minAmount;

    @DecimalMin("0.0")
    private BigDecimal maxAmount;

    private Boolean isActive;
}
