package com.payroute.dto;

import com.payroute.entity.CircuitBreakerState;
import com.payroute.entity.ProviderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderAnalyticsDto {
    private UUID providerId;
    private String providerCode;
    private String providerName;
    private ProviderStatus status;
    private CircuitBreakerState circuitBreakerState;
    private long totalTransactions;
    private long successCount;
    private long failureCount;
    private double successRate;
    private long avgLatencyMs;
    private BigDecimal totalFeesCollected;
    private int priority;
}
