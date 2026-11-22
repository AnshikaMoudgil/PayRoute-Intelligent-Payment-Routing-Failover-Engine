package com.payroute.controller;

import com.payroute.dto.PaymentAttemptDto;
import com.payroute.dto.PaymentInitiateRequest;
import com.payroute.dto.PaymentResponse;
import com.payroute.entity.Merchant;
import com.payroute.service.MerchantService;
import com.payroute.service.PaymentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Processing", description = "Endpoints for initiating transactions, intelligent routing, idempotency, and query")
public class PaymentController {

    private final PaymentProcessingService paymentProcessingService;
    private final MerchantService merchantService;

    @PostMapping
    @Operation(summary = "Initiate payment request", description = "Validates idempotency, selects optimal gateway, executes simulation, retries/fails over if needed")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Parameter(description = "Unique idempotency key to prevent double charge", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Parameter(description = "Optional merchant code override (for API playground/testing)")
            @RequestHeader(value = "X-Merchant-Code", required = false) String merchantCodeHeader,
            @Valid @RequestBody PaymentInitiateRequest request) {

        Merchant merchant = merchantService.resolveCurrentMerchant(merchantCodeHeader);
        PaymentResponse response = paymentProcessingService.processPayment(merchant, idempotencyKey, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment transaction by ID", description = "Retrieves full transaction lifecycle state and attempts")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentProcessingService.getPaymentById(id));
    }

    @GetMapping
    @Operation(summary = "List all payments", description = "Paginated list of payment transactions ordered by creation date descending")
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(paymentProcessingService.getAllPayments(pageRequest));
    }

    @GetMapping("/{id}/attempts")
    @Operation(summary = "Get provider attempts for transaction", description = "Retrieves detailed provider attempt logs, latency, and failovers")
    public ResponseEntity<List<PaymentAttemptDto>> getPaymentAttempts(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentProcessingService.getPaymentAttempts(id));
    }
}
