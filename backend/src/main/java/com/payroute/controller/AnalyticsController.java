package com.payroute.controller;

import com.payroute.dto.AnalyticsOverviewDto;
import com.payroute.dto.PerformanceMetricsDto;
import com.payroute.dto.ProviderAnalyticsDto;
import com.payroute.dto.TransactionVolumeDto;
import com.payroute.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Telemetry", description = "Endpoints for metrics, success rates, latency, transaction volume, and performance")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get analytics overview", description = "Returns top-line KPIs: total volume, success rate, fee sum, retries, and switches")
    public ResponseEntity<AnalyticsOverviewDto> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    @GetMapping("/providers")
    @Operation(summary = "Get provider performance comparison", description = "Returns breakdown of transactions, success rate, latency, and fees per gateway")
    public ResponseEntity<List<ProviderAnalyticsDto>> getProviderAnalytics() {
        return ResponseEntity.ok(analyticsService.getProviderAnalytics());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transaction volume over time", description = "Time-series bucketed volume and counts for chart visualization")
    public ResponseEntity<List<TransactionVolumeDto>> getTransactionVolume(
            @RequestParam(defaultValue = "24") int hoursBack) {
        return ResponseEntity.ok(analyticsService.getTransactionVolumeTimeline(hoursBack));
    }

    @GetMapping("/performance")
    @Operation(summary = "Get system performance metrics", description = "Throughput, average response time, p95, p99 latency")
    public ResponseEntity<PerformanceMetricsDto> getPerformanceMetrics() {
        return ResponseEntity.ok(analyticsService.getPerformanceMetrics());
    }
}
