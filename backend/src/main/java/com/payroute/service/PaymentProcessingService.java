package com.payroute.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroute.circuitbreaker.ProviderCircuitBreakerManager;
import com.payroute.dto.PaymentAttemptDto;
import com.payroute.dto.PaymentInitiateRequest;
import com.payroute.dto.PaymentResponse;
import com.payroute.dto.event.PaymentEventMessage;
import com.payroute.entity.*;
import com.payroute.exception.InvalidPaymentException;
import com.payroute.exception.ProviderUnavailableException;
import com.payroute.exception.ResourceNotFoundException;
import com.payroute.kafka.PaymentEventPublisher;
import com.payroute.provider.PaymentProviderClient;
import com.payroute.provider.PaymentProviderRegistry;
import com.payroute.provider.ProviderTransactionResult;
import com.payroute.repository.PaymentProviderRepository;
import com.payroute.repository.PaymentTransactionRepository;
import com.payroute.repository.ProviderAttemptRepository;
import com.payroute.routing.LowestCostRoutingStrategy;
import com.payroute.routing.RoutingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentTransactionRepository transactionRepository;
    private final ProviderAttemptRepository attemptRepository;
    private final PaymentProviderRepository providerRepository;
    private final RoutingRuleService routingRuleService;
    private final RoutingEngine routingEngine;
    private final PaymentProviderRegistry providerRegistry;
    private final ProviderCircuitBreakerManager circuitBreakerManager;
    private final PaymentEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final LowestCostRoutingStrategy costCalculator;
    private final ObjectMapper objectMapper;

    @Value("${payroute.routing.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${payroute.routing.retry-base-delay-ms:150}")
    private long retryBaseDelayMs;

    /**
     * Executes the complete payment processing lifecycle with idempotency, intelligent routing,
     * retry, failover, and event streaming.
     */
    public PaymentResponse processPayment(Merchant merchant, String idempotencyKey, PaymentInitiateRequest request) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new InvalidPaymentException("Header 'Idempotency-Key' is required for payment processing");
        }
        idempotencyKey = idempotencyKey.trim();

        // 1. Idempotency Check & Lock Acquisition
        Optional<IdempotencyRecord> cached = idempotencyService.acquireOrCheck(merchant, idempotencyKey, request);
        if (cached.isPresent()) {
            try {
                return objectMapper.readValue(cached.get().getResponsePayload(), PaymentResponse.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached idempotency payload, re-fetching transaction: {}", e.getMessage());
                return transactionRepository.findByMerchantAndIdempotencyKey(merchant, idempotencyKey)
                        .map(this::mapToPaymentResponse)
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for cached key"));
            }
        }

        PaymentResponse response;
        try {
            response = executeTransaction(merchant, idempotencyKey, request);
            // Record successful idempotency payload
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                idempotencyService.recordCompletion(merchant, idempotencyKey, responseJson, 200);
            } catch (Exception jsonEx) {
                log.warn("Failed to serialize response for idempotency caching: {}", jsonEx.getMessage());
            }
        } catch (Exception ex) {
            String errorJson = "{\"error\":\"" + ex.getMessage() + "\"}";
            idempotencyService.recordFailure(merchant, idempotencyKey, errorJson, 500);
            throw ex;
        }

        return response;
    }

    @Transactional
    protected PaymentResponse executeTransaction(Merchant merchant, String idempotencyKey, PaymentInitiateRequest request) {
        // 2. Initialize Transaction in DB
        PaymentTransaction transaction = PaymentTransaction.builder()
                .merchant(merchant)
                .idempotencyKey(idempotencyKey)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase() : "INR")
                .status(TransactionStatus.PROCESSING)
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .totalAttempts(0)
                .build();

        transaction = transactionRepository.saveAndFlush(transaction);
        log.info("Initialized transaction id={}, merchant={}, amount={}",
                transaction.getId(), merchant.getMerchantCode(), transaction.getAmount());

        // Publish Payment Initiated Event
        eventPublisher.publish(PaymentEventMessage.builder()
                .transactionId(transaction.getId())
                .merchantId(merchant.getId())
                .merchantCode(merchant.getMerchantCode())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionStatus(TransactionStatus.PROCESSING)
                .eventType(PaymentEventType.PAYMENT_INITIATED)
                .timestamp(LocalDateTime.now())
                .build());

        // 3. Resolve Effective Routing Rule and Candidate Providers
        RoutingRule effectiveRule = routingRuleService.resolveEffectiveRule(merchant, transaction.getAmount());
        RoutingStrategyType strategyType = request.getRoutingStrategyOverride() != null
                ? request.getRoutingStrategyOverride()
                : (effectiveRule != null ? effectiveRule.getStrategy() : merchant.getDefaultRoutingStrategy());

        Map<String, Integer> weights = effectiveRule != null ? routingRuleService.deserializeWeights(effectiveRule.getProviderWeights()) : null;
        transaction.setRoutingStrategyUsed(strategyType.name());

        List<PaymentProvider> allProviders = providerRepository.findAll();
        List<PaymentProvider> candidateProviders = routingEngine.resolveCandidateProviders(
                allProviders, transaction, strategyType, weights);

        if (candidateProviders.isEmpty()) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("No eligible active providers available in the system");
            transactionRepository.save(transaction);
            throw new ProviderUnavailableException("No eligible payment providers available for routing");
        }

        // 4. Execute Payment with Retry and Failover
        int candidateIndex = 0;
        int currentAttemptNumber = 0;
        boolean paymentSuccess = false;
        PaymentProvider successfulProvider = null;
        String finalFailureReason = null;

        while (candidateIndex < candidateProviders.size() && currentAttemptNumber < maxRetryAttempts) {
            PaymentProvider provider = candidateProviders.get(candidateIndex);
            currentAttemptNumber++;
            boolean isFailover = candidateIndex > 0;

            log.info("Attempt #{} on provider {} (isFailover={}) for tx={}",
                    currentAttemptNumber, provider.getCode(), isFailover, transaction.getId());

            PaymentProviderClient client = providerRegistry.getClient(provider.getCode());
            ProviderTransactionResult result = client.processPayment(transaction, provider);

            // Record attempt entity
            ProviderAttempt attempt = ProviderAttempt.builder()
                    .transaction(transaction)
                    .provider(provider)
                    .attemptNumber(currentAttemptNumber)
                    .status(result.isSuccess() ? AttemptStatus.SUCCESS : AttemptStatus.FAILED)
                    .latencyMs(result.getLatencyMs())
                    .errorCode(result.getErrorCode())
                    .errorMessage(result.getErrorMessage())
                    .isFailover(isFailover)
                    .rawResponse(result.getRawResponse())
                    .build();

            attemptRepository.save(attempt);
            transaction.addAttempt(attempt);

            if (result.isSuccess()) {
                paymentSuccess = true;
                successfulProvider = provider;
                circuitBreakerManager.recordSuccess(provider.getCode());
                break;
            } else {
                circuitBreakerManager.recordFailure(provider.getCode());
                finalFailureReason = result.getErrorMessage();

                // Non-retryable / Permanent failure check
                if (!result.isRetryable()) {
                    log.warn("Permanent failure encountered ({}: {}). Aborting retries and failover.",
                            result.getErrorCode(), result.getErrorMessage());
                    break;
                }

                // If retryable and another candidate is available, switch provider (Failover)!
                if (candidateIndex + 1 < candidateProviders.size()) {
                    candidateIndex++;
                    PaymentProvider nextProvider = candidateProviders.get(candidateIndex);
                    log.warn("Triggering FAILOVER from {} to {} for tx={}",
                            provider.getCode(), nextProvider.getCode(), transaction.getId());

                    // Publish PROVIDER_SWITCHED event
                    eventPublisher.publish(PaymentEventMessage.builder()
                            .transactionId(transaction.getId())
                            .merchantId(merchant.getId())
                            .merchantCode(merchant.getMerchantCode())
                            .amount(transaction.getAmount())
                            .currency(transaction.getCurrency())
                            .transactionStatus(TransactionStatus.PROCESSING)
                            .eventType(PaymentEventType.PROVIDER_SWITCHED)
                            .providerCode(nextProvider.getCode())
                            .attemptNumber(currentAttemptNumber)
                            .failureReason("Failover from " + provider.getCode() + " due to " + result.getErrorCode())
                            .timestamp(LocalDateTime.now())
                            .build());

                    // Exponential backoff before next attempt
                    long backoffDelay = (long) (retryBaseDelayMs * Math.pow(2, currentAttemptNumber - 1));
                    try {
                        Thread.sleep(Math.min(backoffDelay, 1000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.warn("No further failover candidates remaining in pool for tx={}", transaction.getId());
                    break;
                }
            }
        }

        // 5. Finalize Transaction State
        if (paymentSuccess && successfulProvider != null) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setSelectedProvider(successfulProvider);
            BigDecimal fee = costCalculator.calculateFee(successfulProvider, transaction.getAmount());
            transaction.setFeeCharged(fee);
            transaction.setFailureReason(null);
            transaction = transactionRepository.save(transaction);

            eventPublisher.publish(PaymentEventMessage.builder()
                    .transactionId(transaction.getId())
                    .merchantId(merchant.getId())
                    .merchantCode(merchant.getMerchantCode())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .transactionStatus(TransactionStatus.SUCCESS)
                    .eventType(PaymentEventType.PAYMENT_SUCCESS)
                    .providerCode(successfulProvider.getCode())
                    .attemptNumber(transaction.getTotalAttempts())
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(finalFailureReason != null ? finalFailureReason : "Transaction failed across all attempted providers");
            transaction = transactionRepository.save(transaction);

            eventPublisher.publish(PaymentEventMessage.builder()
                    .transactionId(transaction.getId())
                    .merchantId(merchant.getId())
                    .merchantCode(merchant.getMerchantCode())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .transactionStatus(TransactionStatus.FAILED)
                    .eventType(PaymentEventType.PAYMENT_FAILED)
                    .attemptNumber(transaction.getTotalAttempts())
                    .failureReason(transaction.getFailureReason())
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        return mapToPaymentResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        PaymentTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
        return mapToPaymentResponse(tx);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(this::mapToPaymentResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMerchantPayments(Merchant merchant, Pageable pageable) {
        return transactionRepository.findByMerchantOrderByCreatedAtDesc(merchant, pageable)
                .map(this::mapToPaymentResponse);
    }

    @Transactional(readOnly = true)
    public List<PaymentAttemptDto> getPaymentAttempts(UUID transactionId) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        return attemptRepository.findByTransactionOrderByAttemptNumberAsc(tx).stream()
                .map(this::mapAttemptToDto)
                .collect(Collectors.toList());
    }

    public PaymentResponse mapToPaymentResponse(PaymentTransaction tx) {
        List<PaymentAttemptDto> attempts = tx.getAttempts() != null
                ? tx.getAttempts().stream().map(this::mapAttemptToDto).collect(Collectors.toList())
                : Collections.emptyList();

        return PaymentResponse.builder()
                .id(tx.getId())
                .merchantId(tx.getMerchant() != null ? tx.getMerchant().getId() : null)
                .merchantCode(tx.getMerchant() != null ? tx.getMerchant().getMerchantCode() : null)
                .merchantName(tx.getMerchant() != null ? tx.getMerchant().getName() : null)
                .idempotencyKey(tx.getIdempotencyKey())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .selectedProviderId(tx.getSelectedProvider() != null ? tx.getSelectedProvider().getId() : null)
                .selectedProviderCode(tx.getSelectedProvider() != null ? tx.getSelectedProvider().getCode() : null)
                .selectedProviderName(tx.getSelectedProvider() != null ? tx.getSelectedProvider().getName() : null)
                .routingStrategyUsed(tx.getRoutingStrategyUsed())
                .totalAttempts(tx.getTotalAttempts())
                .feeCharged(tx.getFeeCharged())
                .customerId(tx.getCustomerId())
                .customerEmail(tx.getCustomerEmail())
                .failureReason(tx.getFailureReason())
                .attempts(attempts)
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }

    public PaymentAttemptDto mapAttemptToDto(ProviderAttempt a) {
        return PaymentAttemptDto.builder()
                .id(a.getId())
                .providerId(a.getProvider() != null ? a.getProvider().getId() : null)
                .providerCode(a.getProvider() != null ? a.getProvider().getCode() : null)
                .providerName(a.getProvider() != null ? a.getProvider().getName() : null)
                .attemptNumber(a.getAttemptNumber())
                .status(a.getStatus())
                .latencyMs(a.getLatencyMs())
                .errorCode(a.getErrorCode())
                .errorMessage(a.getErrorMessage())
                .isFailover(a.getIsFailover())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
