package com.payroute.service;

import com.payroute.dto.RefundCreateRequest;
import com.payroute.dto.RefundResponse;
import com.payroute.entity.*;
import com.payroute.kafka.PaymentEventPublisher;
import com.payroute.provider.PaymentProviderRegistry;
import com.payroute.provider.SimulatedProviderAClient;
import com.payroute.exception.RefundException;
import com.payroute.repository.PaymentTransactionRepository;
import com.payroute.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private PaymentTransactionRepository transactionRepository;
    @Mock
    private PaymentEventPublisher eventPublisher;

    private PaymentProviderRegistry providerRegistry;
    private RefundService refundService;
    private PaymentTransaction transaction;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        providerRegistry = new PaymentProviderRegistry(List.of(new SimulatedProviderAClient()));
        refundService = new RefundService(refundRepository, transactionRepository, providerRegistry, eventPublisher);

        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .merchantCode("MERCH-ACME")
                .build();

        PaymentProvider providerA = PaymentProvider.builder()
                .id(UUID.randomUUID())
                .code("PROVIDER_A")
                .name("ApexPay")
                .build();

        transaction = PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .selectedProvider(providerA)
                .status(TransactionStatus.SUCCESS)
                .build();
    }

    @Test
    @DisplayName("Reject refund on FAILED transaction")
    void testRefundOnFailedTransaction() {
        transaction.setStatus(TransactionStatus.FAILED);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        RefundCreateRequest request = RefundCreateRequest.builder()
                .transactionId(transaction.getId())
                .amount(new BigDecimal("500.00"))
                .build();

        assertThrows(RefundException.class, () -> refundService.initiateRefund(request));
    }

    @Test
    @DisplayName("Reject refund exceeding transaction amount")
    void testRefundExceedingAmount() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(refundRepository.sumSuccessfulRefundsByTransaction(transaction)).thenReturn(new BigDecimal("600.00"));

        // Only 400 available, request is 500
        RefundCreateRequest request = RefundCreateRequest.builder()
                .transactionId(transaction.getId())
                .amount(new BigDecimal("500.00"))
                .build();

        assertThrows(RefundException.class, () -> refundService.initiateRefund(request));
    }

    @Test
    @DisplayName("Full refund updates transaction status to REFUNDED")
    void testFullRefundUpdatesStatus() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(refundRepository.sumSuccessfulRefundsByTransaction(transaction)).thenReturn(BigDecimal.ZERO);
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundCreateRequest request = RefundCreateRequest.builder()
                .transactionId(transaction.getId())
                .amount(new BigDecimal("1000.00"))
                .reason("Customer requested total refund")
                .build();

        RefundResponse response = refundService.initiateRefund(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.REFUNDED, transaction.getStatus());
        verify(transactionRepository, times(1)).save(transaction);
        verify(eventPublisher, times(1)).publish(any());
    }
}
