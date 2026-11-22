package com.payroute.controller.graphql;

import com.payroute.dto.*;
import com.payroute.entity.CircuitBreakerState;
import com.payroute.entity.Merchant;
import com.payroute.entity.ProviderStatus;
import com.payroute.entity.RoutingStrategyType;
import com.payroute.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PayRouteGraphQLController {

    private final MerchantService merchantService;
    private final PaymentProviderService providerService;
    private final RoutingRuleService routingRuleService;
    private final PaymentProcessingService paymentProcessingService;
    private final RefundService refundService;
    private final AnalyticsService analyticsService;

    // Queries
    @QueryMapping
    public List<MerchantResponse> merchants() {
        return merchantService.getAllMerchants();
    }

    @QueryMapping
    public MerchantResponse merchantById(@Argument UUID id) {
        return merchantService.getMerchantById(id);
    }

    @QueryMapping
    public List<ProviderDto> providers() {
        return providerService.getAllProviders();
    }

    @QueryMapping
    public ProviderDto providerById(@Argument UUID id) {
        return providerService.getProviderById(id);
    }

    @QueryMapping
    public List<RoutingRuleDto> routingRules() {
        return routingRuleService.getAllRules();
    }

    @QueryMapping
    public List<PaymentResponse> payments(@Argument int page, @Argument int size) {
        int effectivePage = Math.max(0, page);
        int effectiveSize = size > 0 ? size : 20;
        PageRequest pr = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentProcessingService.getAllPayments(pr).getContent();
    }

    @QueryMapping
    public PaymentResponse paymentById(@Argument UUID id) {
        return paymentProcessingService.getPaymentById(id);
    }

    @QueryMapping
    public List<PaymentAttemptDto> paymentAttempts(@Argument UUID transactionId) {
        return paymentProcessingService.getPaymentAttempts(transactionId);
    }

    @QueryMapping
    public List<RefundResponse> refunds(@Argument int page, @Argument int size) {
        int effectivePage = Math.max(0, page);
        int effectiveSize = size > 0 ? size : 20;
        PageRequest pr = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return refundService.getAllRefunds(pr).getContent();
    }

    @QueryMapping
    public RefundResponse refundById(@Argument UUID id) {
        return refundService.getRefundById(id);
    }

    @QueryMapping
    public AnalyticsOverviewDto analyticsOverview() {
        return analyticsService.getOverview();
    }

    @QueryMapping
    public List<ProviderAnalyticsDto> providerAnalytics() {
        return analyticsService.getProviderAnalytics();
    }

    // Mutations
    @MutationMapping
    public PaymentResponse initiatePayment(@Argument Map<String, Object> input) {
        String idempotencyKey = (String) input.get("idempotencyKey");
        String merchantCode = (String) input.get("merchantCode");
        Number amountNum = (Number) input.get("amount");
        String currency = (String) input.getOrDefault("currency", "INR");
        String customerId = (String) input.get("customerId");
        String customerEmail = (String) input.get("customerEmail");
        String strategyOverrideStr = (String) input.get("routingStrategyOverride");

        Merchant merchant = merchantService.resolveCurrentMerchant(merchantCode);

        PaymentInitiateRequest request = PaymentInitiateRequest.builder()
                .amount(BigDecimal.valueOf(amountNum.doubleValue()))
                .currency(currency)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .routingStrategyOverride(strategyOverrideStr != null ? RoutingStrategyType.valueOf(strategyOverrideStr) : null)
                .build();

        return paymentProcessingService.processPayment(merchant, idempotencyKey, request);
    }

    @MutationMapping
    public RefundResponse initiateRefund(@Argument Map<String, Object> input) {
        UUID transactionId = UUID.fromString((String) input.get("transactionId"));
        Number amountNum = (Number) input.get("amount");
        String reason = (String) input.get("reason");

        RefundCreateRequest request = RefundCreateRequest.builder()
                .transactionId(transactionId)
                .amount(BigDecimal.valueOf(amountNum.doubleValue()))
                .reason(reason)
                .build();

        return refundService.initiateRefund(request);
    }

    @MutationMapping
    public ProviderDto updateProviderHealth(@Argument Map<String, Object> input) {
        UUID providerId = UUID.fromString((String) input.get("providerId"));
        String statusStr = (String) input.get("status");
        String cbStateStr = (String) input.get("circuitBreakerState");
        Boolean resetCb = (Boolean) input.get("resetCircuitBreaker");

        ProviderHealthUpdateDto updateDto = ProviderHealthUpdateDto.builder()
                .status(statusStr != null ? ProviderStatus.valueOf(statusStr) : null)
                .circuitBreakerState(cbStateStr != null ? CircuitBreakerState.valueOf(cbStateStr) : null)
                .resetCircuitBreaker(resetCb)
                .build();

        return providerService.updateProviderHealth(providerId, updateDto);
    }
}
