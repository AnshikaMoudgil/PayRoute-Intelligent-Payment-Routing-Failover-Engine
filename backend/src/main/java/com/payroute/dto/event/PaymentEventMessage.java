package com.payroute.dto.event;

import com.payroute.entity.PaymentEventType;
import com.payroute.entity.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEventMessage {
    private UUID eventId;
    private UUID transactionId;
    private PaymentEventType eventType;
    private UUID merchantId;
    private String merchantCode;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus transactionStatus;
    private String providerCode;
    private Integer attemptNumber;
    private String failureReason;
    private LocalDateTime timestamp;
}
