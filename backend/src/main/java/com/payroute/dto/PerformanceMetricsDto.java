package com.payroute.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceMetricsDto {
    private double requestsPerSecond;
    private double avgResponseTimeMs;
    private double p50ResponseTimeMs;
    private double p95ResponseTimeMs;
    private double p99ResponseTimeMs;
    private double failureRatePercentage;
    private long totalRequestsSampled;
    private Map<String, Object> details;
}
