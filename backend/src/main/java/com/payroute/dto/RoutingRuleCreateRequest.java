package com.payroute.dto;

import com.payroute.entity.RoutingStrategyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRuleCreateRequest {

    private UUID merchantId; // null indicates global rule

    @NotNull(message = "Routing strategy is required")
    private RoutingStrategyType strategy;

    private Map<String, Integer> providerWeights;

    @DecimalMin("0.0")
    @Builder.Default
    private BigDecimal minAmount = BigDecimal.ZERO;

    @DecimalMin("0.0")
    @Builder.Default
    private BigDecimal maxAmount = new BigDecimal("100000000.00");

    @Builder.Default
    private Boolean isActive = true;
}
