package com.payroute.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsOverviewDto {
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long pendingTransactions;
    private double overallSuccessRate;
    private BigDecimal totalVolume;
    private BigDecimal totalProcessingFees;
    private long totalRetries;
    private long totalProviderSwitches;
    private List<ProviderAnalyticsDto> providerPerformance;
}
