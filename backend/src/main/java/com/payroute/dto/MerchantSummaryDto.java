package com.payroute.dto;

import com.payroute.entity.ProviderStatus;
import com.payroute.entity.RoutingStrategyType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantSummaryDto {
    private UUID id;
    private String merchantCode;
    private String name;
    private String email;
    private String rawApiKeyPrefix;
    private ProviderStatus status;
    private RoutingStrategyType defaultRoutingStrategy;
}
