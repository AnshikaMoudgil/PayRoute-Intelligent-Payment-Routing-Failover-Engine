package com.payroute.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_code", nullable = false, unique = true, length = 50)
    private String merchantCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "api_key_hash", nullable = false, unique = true, length = 128)
    private String apiKeyHash;

    @Column(name = "raw_api_key_prefix", nullable = false, length = 20)
    private String rawApiKeyPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProviderStatus status = ProviderStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_routing_strategy", nullable = false, length = 50)
    @Builder.Default
    private RoutingStrategyType defaultRoutingStrategy = RoutingStrategyType.PRIORITY;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
