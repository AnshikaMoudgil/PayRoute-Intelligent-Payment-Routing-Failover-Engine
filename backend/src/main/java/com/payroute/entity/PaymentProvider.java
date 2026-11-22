package com.payroute.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProviderStatus status = ProviderStatus.ACTIVE;

    @Column(name = "base_fee_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal baseFeePercentage = new BigDecimal("1.50");

    @Column(name = "base_fee_flat", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseFeeFlat = new BigDecimal("2.00");

    @Column(name = "avg_latency_ms", nullable = false)
    @Builder.Default
    private Long avgLatencyMs = 150L;

    @Column(name = "simulated_success_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal simulatedSuccessRate = new BigDecimal("95.00");

    @Column(name = "failure_probability", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal failureProbability = new BigDecimal("5.00");

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
