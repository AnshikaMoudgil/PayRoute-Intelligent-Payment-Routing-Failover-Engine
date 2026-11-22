package com.payroute.service;

import com.payroute.dto.MerchantCreateRequest;
import com.payroute.dto.MerchantResponse;
import com.payroute.entity.Merchant;
import com.payroute.entity.ProviderStatus;
import com.payroute.exception.DuplicateResourceException;
import com.payroute.exception.ResourceNotFoundException;
import com.payroute.repository.MerchantRepository;
import com.payroute.security.ApiKeyAuthenticationFilter;
import com.payroute.security.MerchantPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MerchantResponse registerMerchant(MerchantCreateRequest request) {
        if (merchantRepository.existsByMerchantCode(request.getMerchantCode())) {
            throw new DuplicateResourceException("Merchant with code " + request.getMerchantCode() + " already exists");
        }
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Merchant with email " + request.getEmail() + " already exists");
        }

        // Generate a secure raw API key: pr_live_<random_hex>
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String rawKey = "pr_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String keyHash = ApiKeyAuthenticationFilter.hashSha256(rawKey);
        String keyPrefix = rawKey.substring(0, Math.min(rawKey.length(), 14)) + "...";

        Merchant merchant = Merchant.builder()
                .merchantCode(request.getMerchantCode().trim().toUpperCase())
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .apiKeyHash(keyHash)
                .rawApiKeyPrefix(keyPrefix)
                .status(ProviderStatus.ACTIVE)
                .defaultRoutingStrategy(request.getDefaultRoutingStrategy())
                .build();

        Merchant saved = merchantRepository.save(merchant);
        log.info("Registered new merchant: code={}, id={}", saved.getMerchantCode(), saved.getId());

        MerchantResponse response = mapToResponse(saved);
        response.setRawApiKey(rawKey); // Only returned once upon creation!
        return response;
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> getAllMerchants() {
        return merchantRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchantById(UUID id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with ID: " + id));
        return mapToResponse(merchant);
    }

    @Transactional(readOnly = true)
    public Merchant getEntityById(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Merchant resolveCurrentMerchant(String merchantCodeHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof MerchantPrincipal principal) {
            return principal.getMerchant();
        }

        if (merchantCodeHeader != null && !merchantCodeHeader.isBlank()) {
            return merchantRepository.findByMerchantCode(merchantCodeHeader.trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found for code: " + merchantCodeHeader));
        }

        // Default sandbox merchant if available (for playground ease)
        return merchantRepository.findByMerchantCode("MERCH-ACME-001")
                .orElseThrow(() -> new ResourceNotFoundException("No authenticated merchant found and default sandbox merchant is unavailable"));
    }

    public MerchantResponse mapToResponse(Merchant m) {
        return MerchantResponse.builder()
                .id(m.getId())
                .merchantCode(m.getMerchantCode())
                .name(m.getName())
                .email(m.getEmail())
                .rawApiKeyPrefix(m.getRawApiKeyPrefix())
                .status(m.getStatus())
                .defaultRoutingStrategy(m.getDefaultRoutingStrategy())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
