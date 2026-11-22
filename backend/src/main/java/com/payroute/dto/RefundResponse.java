package com.payroute.dto;

import com.payroute.entity.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private UUID id;
    private UUID transactionId;
    private UUID merchantId;
    private String merchantCode;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private String providerRefundId;
    private LocalDateTime createdAt;
}
