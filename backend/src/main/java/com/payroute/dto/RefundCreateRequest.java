package com.payroute.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundCreateRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.50", message = "Refund amount must be at least 0.50")
    private BigDecimal amount;

    private String reason;
}
