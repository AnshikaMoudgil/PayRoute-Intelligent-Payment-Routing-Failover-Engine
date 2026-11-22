package com.payroute.dto;

import com.payroute.entity.ProviderStatus;
import com.payroute.entity.RoutingStrategyType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantResponse {
    private UUID id;
    private String merchantCode;
    private String name;
    private String email;
    private String rawApiKey; // Only returned once upon creation or masked
    private String rawApiKeyPrefix;
    private ProviderStatus status;
    private RoutingStrategyType defaultRoutingStrategy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
