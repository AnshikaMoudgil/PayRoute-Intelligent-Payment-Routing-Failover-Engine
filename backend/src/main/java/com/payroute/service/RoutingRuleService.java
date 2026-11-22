package com.payroute.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroute.config.CacheConfig;
import com.payroute.dto.RoutingRuleCreateRequest;
import com.payroute.dto.RoutingRuleDto;
import com.payroute.dto.RoutingRuleUpdateRequest;
import com.payroute.entity.Merchant;
import com.payroute.entity.RoutingRule;
import com.payroute.entity.RoutingStrategyType;
import com.payroute.exception.ResourceNotFoundException;
import com.payroute.repository.MerchantRepository;
import com.payroute.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingRuleService {

    private final RoutingRuleRepository routingRuleRepository;
    private final MerchantRepository merchantRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<RoutingRuleDto> getAllRules() {
        return routingRuleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoutingRuleDto getRuleById(UUID id) {
        RoutingRule rule = routingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Routing rule not found with ID: " + id));
        return mapToDto(rule);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ROUTING_RULES, allEntries = true)
    public RoutingRuleDto createRule(RoutingRuleCreateRequest request) {
        Merchant merchant = null;
        if (request.getMerchantId() != null) {
            merchant = merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with ID: " + request.getMerchantId()));
        }

        String weightsJson = serializeWeights(request.getProviderWeights());

        RoutingRule rule = RoutingRule.builder()
                .merchant(merchant)
                .strategy(request.getStrategy())
                .providerWeights(weightsJson)
                .minAmount(request.getMinAmount() != null ? request.getMinAmount() : BigDecimal.ZERO)
                .maxAmount(request.getMaxAmount() != null ? request.getMaxAmount() : new BigDecimal("100000000.00"))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        RoutingRule saved = routingRuleRepository.save(rule);
        log.info("Created routing rule: id={}, strategy={}, merchant={}",
                saved.getId(), saved.getStrategy(), (merchant != null ? merchant.getMerchantCode() : "GLOBAL"));
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ROUTING_RULES, allEntries = true)
    public RoutingRuleDto updateRule(UUID id, RoutingRuleUpdateRequest request) {
        RoutingRule rule = routingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Routing rule not found with ID: " + id));

        if (request.getStrategy() != null) rule.setStrategy(request.getStrategy());
        if (request.getProviderWeights() != null) rule.setProviderWeights(serializeWeights(request.getProviderWeights()));
        if (request.getMinAmount() != null) rule.setMinAmount(request.getMinAmount());
        if (request.getMaxAmount() != null) rule.setMaxAmount(request.getMaxAmount());
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());

        RoutingRule updated = routingRuleRepository.save(rule);
        log.info("Updated routing rule: id={}", updated.getId());
        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public RoutingRule resolveEffectiveRule(Merchant merchant, BigDecimal amount) {
        if (merchant != null) {
            List<RoutingRule> merchantRules = routingRuleRepository.findByMerchantAndIsActiveTrue(merchant);
            for (RoutingRule rule : merchantRules) {
                if (isAmountInRange(rule, amount)) {
                    return rule;
                }
            }
        }

        // Global default fallback
        List<RoutingRule> globalRules = routingRuleRepository.findByMerchantIsNullAndIsActiveTrue();
        for (RoutingRule rule : globalRules) {
            if (isAmountInRange(rule, amount)) {
                return rule;
            }
        }

        return null;
    }

    private boolean isAmountInRange(RoutingRule rule, BigDecimal amount) {
        if (amount == null) return true;
        boolean minOk = rule.getMinAmount() == null || amount.compareTo(rule.getMinAmount()) >= 0;
        boolean maxOk = rule.getMaxAmount() == null || amount.compareTo(rule.getMaxAmount()) <= 0;
        return minOk && maxOk;
    }

    public RoutingRuleDto mapToDto(RoutingRule r) {
        return RoutingRuleDto.builder()
                .id(r.getId())
                .merchantId(r.getMerchant() != null ? r.getMerchant().getId() : null)
                .merchantCode(r.getMerchant() != null ? r.getMerchant().getMerchantCode() : "GLOBAL")
                .merchantName(r.getMerchant() != null ? r.getMerchant().getName() : "Global Default")
                .strategy(r.getStrategy())
                .providerWeights(deserializeWeights(r.getProviderWeights()))
                .minAmount(r.getMinAmount())
                .maxAmount(r.getMaxAmount())
                .isActive(r.getIsActive())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public String serializeWeights(Map<String, Integer> weights) {
        if (weights == null || weights.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(weights);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Integer> deserializeWeights(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
