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
public class ProviderCreateRequest {

    @NotBlank(message = "Provider code is required")
    @Size(min = 2, max = 50)
    private String code;

    @NotBlank(message = "Provider name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Builder.Default
    private ProviderStatus status = ProviderStatus.ACTIVE;

    @NotNull(message = "Base fee percentage is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal baseFeePercentage;

    @NotNull(message = "Base fee flat is required")
    @DecimalMin(value = "0.0")
    private BigDecimal baseFeeFlat;

    @NotNull(message = "Average latency is required")
    @Min(10)
    @Max(10000)
    private Long avgLatencyMs;

    @NotNull(message = "Simulated success rate is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal simulatedSuccessRate;

    @NotNull(message = "Failure probability is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal failureProbability;

    @NotNull(message = "Priority is required")
    @Min(1)
    private Integer priority;
}
