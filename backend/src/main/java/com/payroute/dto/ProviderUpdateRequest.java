package com.payroute.dto;

import com.payroute.entity.ProviderStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderUpdateRequest {

    @Size(min = 2, max = 100)
    private String name;

    private ProviderStatus status;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal baseFeePercentage;

    @DecimalMin(value = "0.0")
    private BigDecimal baseFeeFlat;

    @Min(10)
    @Max(10000)
    private Long avgLatencyMs;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal simulatedSuccessRate;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal failureProbability;

    @Min(1)
    private Integer priority;
}
