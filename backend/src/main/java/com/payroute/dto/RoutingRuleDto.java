package com.payroute.dto;

import com.payroute.entity.RoutingStrategyType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRuleDto {
    private UUID id;
    private UUID merchantId;
    private String merchantCode;
    private String merchantName;
    private RoutingStrategyType strategy;
    private Map<String, Integer> providerWeights;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
