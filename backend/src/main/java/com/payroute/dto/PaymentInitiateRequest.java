package com.payroute.dto;

import com.payroute.entity.RoutingStrategyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.50", message = "Amount must be at least 0.50")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    private String customerId;

    private String customerEmail;

    private RoutingStrategyType routingStrategyOverride;

    private String description;
}
