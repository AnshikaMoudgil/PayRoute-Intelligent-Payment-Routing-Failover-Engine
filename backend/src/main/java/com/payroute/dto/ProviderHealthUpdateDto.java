package com.payroute.dto;

import com.payroute.entity.CircuitBreakerState;
import com.payroute.entity.ProviderStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderHealthUpdateDto {
    private ProviderStatus status;
    private CircuitBreakerState circuitBreakerState;
    private Boolean resetCircuitBreaker;
}
