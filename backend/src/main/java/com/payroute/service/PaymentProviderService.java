package com.payroute.service;

import com.payroute.circuitbreaker.ProviderCircuitBreakerManager;
import com.payroute.config.CacheConfig;
import com.payroute.dto.ProviderCreateRequest;
import com.payroute.dto.ProviderDto;
import com.payroute.dto.ProviderHealthUpdateDto;
import com.payroute.dto.ProviderUpdateRequest;
import com.payroute.entity.CircuitBreakerState;
import com.payroute.entity.PaymentProvider;
import com.payroute.entity.ProviderStatus;
import com.payroute.exception.DuplicateResourceException;
import com.payroute.exception.ResourceNotFoundException;
import com.payroute.repository.PaymentProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProviderService {

    private final PaymentProviderRepository providerRepository;
    private final ProviderCircuitBreakerManager circuitBreakerManager;

    @Transactional(readOnly = true)
    public List<ProviderDto> getAllProviders() {
        return providerRepository.findAll().stream()
                .map(this::mapToDtoWithHealth)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentProvider> getActiveProviders() {
        return providerRepository.findByStatus(ProviderStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public ProviderDto getProviderById(UUID id) {
        PaymentProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with ID: " + id));
        return mapToDtoWithHealth(provider);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PROVIDERS, allEntries = true)
    public ProviderDto createProvider(ProviderCreateRequest request) {
        if (providerRepository.findByCode(request.getCode().toUpperCase()).isPresent()) {
            throw new DuplicateResourceException("Provider code already exists: " + request.getCode());
        }

        PaymentProvider provider = PaymentProvider.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .status(request.getStatus() != null ? request.getStatus() : ProviderStatus.ACTIVE)
                .baseFeePercentage(request.getBaseFeePercentage())
                .baseFeeFlat(request.getBaseFeeFlat())
                .avgLatencyMs(request.getAvgLatencyMs())
                .simulatedSuccessRate(request.getSimulatedSuccessRate())
                .failureProbability(request.getFailureProbability())
                .priority(request.getPriority())
                .build();

        PaymentProvider saved = providerRepository.save(provider);
        log.info("Created new payment provider: code={}, id={}", saved.getCode(), saved.getId());
        return mapToDtoWithHealth(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PROVIDERS, allEntries = true)
    public ProviderDto updateProvider(UUID id, ProviderUpdateRequest request) {
        PaymentProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with ID: " + id));

        if (request.getName() != null) provider.setName(request.getName().trim());
        if (request.getStatus() != null) provider.setStatus(request.getStatus());
        if (request.getBaseFeePercentage() != null) provider.setBaseFeePercentage(request.getBaseFeePercentage());
        if (request.getBaseFeeFlat() != null) provider.setBaseFeeFlat(request.getBaseFeeFlat());
        if (request.getAvgLatencyMs() != null) provider.setAvgLatencyMs(request.getAvgLatencyMs());
        if (request.getSimulatedSuccessRate() != null) provider.setSimulatedSuccessRate(request.getSimulatedSuccessRate());
        if (request.getFailureProbability() != null) provider.setFailureProbability(request.getFailureProbability());
        if (request.getPriority() != null) provider.setPriority(request.getPriority());

        PaymentProvider updated = providerRepository.save(provider);
        log.info("Updated provider: code={}", updated.getCode());
        return mapToDtoWithHealth(updated);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PROVIDERS, allEntries = true)
    public ProviderDto updateProviderHealth(UUID id, ProviderHealthUpdateDto request) {
        PaymentProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with ID: " + id));

        if (request.getStatus() != null) {
            provider.setStatus(request.getStatus());
            log.info("Changed provider {} status to {}", provider.getCode(), request.getStatus());
        }

        if (Boolean.TRUE.equals(request.getResetCircuitBreaker())) {
            circuitBreakerManager.resetCircuit(provider.getCode());
            log.info("Reset circuit breaker for provider {}", provider.getCode());
        } else if (request.getCircuitBreakerState() == CircuitBreakerState.OPEN) {
            circuitBreakerManager.tripCircuit(provider.getCode());
            log.info("Manually tripped circuit breaker for provider {}", provider.getCode());
        }

        PaymentProvider saved = providerRepository.save(provider);
        return mapToDtoWithHealth(saved);
    }

    public ProviderDto mapToDtoWithHealth(PaymentProvider p) {
        CircuitBreakerState cbState = circuitBreakerManager.getState(p.getCode());
        int consecutiveFailures = circuitBreakerManager.getConsecutiveFailures(p.getCode());

        return ProviderDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .status(p.getStatus())
                .baseFeePercentage(p.getBaseFeePercentage())
                .baseFeeFlat(p.getBaseFeeFlat())
                .avgLatencyMs(p.getAvgLatencyMs())
                .simulatedSuccessRate(p.getSimulatedSuccessRate())
                .failureProbability(p.getFailureProbability())
                .priority(p.getPriority())
                .circuitBreakerState(cbState)
                .consecutiveFailures(consecutiveFailures)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
