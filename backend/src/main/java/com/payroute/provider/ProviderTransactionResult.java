package com.payroute.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderTransactionResult {
    private boolean success;
    private String providerTransactionId;
    private long latencyMs;
    private String errorCode;
    private String errorMessage;
    private boolean retryable;
    private String rawResponse;
}
