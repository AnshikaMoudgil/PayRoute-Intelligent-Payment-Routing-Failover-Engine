package com.payroute.dto;

import com.payroute.entity.AttemptStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAttemptDto {
    private UUID id;
    private UUID providerId;
    private String providerCode;
    private String providerName;
    private Integer attemptNumber;
    private AttemptStatus status;
    private Long latencyMs;
    private String errorCode;
    private String errorMessage;
    private Boolean isFailover;
    private LocalDateTime createdAt;
}
