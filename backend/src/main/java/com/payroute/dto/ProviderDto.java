package com.payroute.dto;

import com.payroute.entity.CircuitBreakerState;
import com.payroute.entity.ProviderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderDto {
    private UUID id;
    private String code;
    private String name;
    private ProviderStatus status;
    private BigDecimal baseFeePercentage;
    private BigDecimal baseFeeFlat;
    private Long avgLatencyMs;
    private BigDecimal simulatedSuccessRate;
    private BigDecimal failureProbability;
    private Integer priority;
    private CircuitBreakerState circuitBreakerState;
    private Integer consecutiveFailures;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
