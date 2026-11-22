package com.payroute.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionVolumeDto {
    private String timeBucket;
    private long totalCount;
    private long successCount;
    private long failedCount;
    private BigDecimal volume;
}
