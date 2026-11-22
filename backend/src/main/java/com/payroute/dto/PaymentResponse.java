package com.payroute.dto;

import com.payroute.entity.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID merchantId;
    private String merchantCode;
    private String merchantName;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private UUID selectedProviderId;
    private String selectedProviderCode;
    private String selectedProviderName;
    private String routingStrategyUsed;
    private Integer totalAttempts;
    private BigDecimal feeCharged;
    private String customerId;
    private String customerEmail;
    private String failureReason;
    private List<PaymentAttemptDto> attempts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
