package com.payroute.service;

import com.payroute.dto.RefundCreateRequest;
import com.payroute.dto.RefundResponse;
import com.payroute.dto.event.PaymentEventMessage;
import com.payroute.entity.*;
import com.payroute.exception.RefundException;
import com.payroute.exception.ResourceNotFoundException;
import com.payroute.kafka.PaymentEventPublisher;
import com.payroute.provider.PaymentProviderClient;
import com.payroute.provider.PaymentProviderRegistry;
import com.payroute.provider.ProviderTransactionResult;
import com.payroute.repository.PaymentTransactionRepository;
import com.payroute.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProviderRegistry providerRegistry;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public RefundResponse initiateRefund(RefundCreateRequest request) {
        PaymentTransaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + request.getTransactionId()));

        if (transaction.getStatus() != TransactionStatus.SUCCESS && transaction.getStatus() != TransactionStatus.REFUNDED) {
            throw new RefundException("Refund can only be issued for successful transactions. Current status: " + transaction.getStatus());
        }

        BigDecimal existingRefunds = refundRepository.sumSuccessfulRefundsByTransaction(transaction);
        BigDecimal maxAvailable = transaction.getAmount().subtract(existingRefunds);

        if (request.getAmount().compareTo(maxAvailable) > 0) {
            throw new RefundException(String.format(
                    "Requested refund amount (%s %s) exceeds remaining refundable amount (%s %s). Original tx amount: %s",
                    request.getAmount(), transaction.getCurrency(), maxAvailable, transaction.getCurrency(), transaction.getAmount()));
        }

        PaymentProvider provider = transaction.getSelectedProvider();
        String providerRefundId = "ref_sim_" + UUID.randomUUID().toString().substring(0, 10);

        if (provider != null) {
            try {
                PaymentProviderClient client = providerRegistry.getClient(provider.getCode());
                Refund temp = Refund.builder().amount(request.getAmount()).build();
                ProviderTransactionResult res = client.processRefund(temp, provider);
                if (res != null && res.getProviderTransactionId() != null) {
                    providerRefundId = res.getProviderTransactionId();
                }
            } catch (Exception e) {
                log.warn("Simulated provider refund call exception: {}", e.getMessage());
            }
        }

        Refund refund = Refund.builder()
                .transaction(transaction)
                .merchant(transaction.getMerchant())
                .amount(request.getAmount())
                .reason(request.getReason() != null ? request.getReason() : "Customer requested refund")
                .status(RefundStatus.SUCCESS)
                .providerRefundId(providerRefundId)
                .build();

        Refund saved = refundRepository.save(refund);

        // Update transaction status if total refunds equal transaction amount
        BigDecimal newTotalRefunded = existingRefunds.add(request.getAmount());
        if (newTotalRefunded.compareTo(transaction.getAmount()) >= 0) {
            transaction.setStatus(TransactionStatus.REFUNDED);
            transactionRepository.save(transaction);
            log.info("Transaction {} fully refunded. Status updated to REFUNDED", transaction.getId());
        }

        // Publish Event
        eventPublisher.publish(PaymentEventMessage.builder()
                .transactionId(transaction.getId())
                .merchantId(transaction.getMerchant().getId())
                .merchantCode(transaction.getMerchant().getMerchantCode())
                .amount(request.getAmount())
                .currency(transaction.getCurrency())
                .transactionStatus(transaction.getStatus())
                .eventType(PaymentEventType.REFUND_INITIATED)
                .providerCode(provider != null ? provider.getCode() : "UNKNOWN")
                .timestamp(LocalDateTime.now())
                .build());

        log.info("Processed refund id={}, txId={}, amount={}", saved.getId(), transaction.getId(), saved.getAmount());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefundById(UUID id) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with ID: " + id));
        return mapToResponse(refund);
    }

    @Transactional(readOnly = true)
    public Page<RefundResponse> getAllRefunds(Pageable pageable) {
        return refundRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public RefundResponse mapToResponse(Refund r) {
        return RefundResponse.builder()
                .id(r.getId())
                .transactionId(r.getTransaction().getId())
                .merchantId(r.getMerchant().getId())
                .merchantCode(r.getMerchant().getMerchantCode())
                .amount(r.getAmount())
                .reason(r.getReason())
                .status(r.getStatus())
                .providerRefundId(r.getProviderRefundId())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
