package com.payroute.controller;

import com.payroute.dto.ProviderCreateRequest;
import com.payroute.dto.ProviderDto;
import com.payroute.dto.ProviderHealthUpdateDto;
import com.payroute.dto.ProviderUpdateRequest;
import com.payroute.service.PaymentProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
@Tag(name = "Payment Provider Management", description = "Endpoints for configuring simulated gateways, health, and circuit breakers")
public class PaymentProviderController {

    private final PaymentProviderService providerService;

    @GetMapping
    @Operation(summary = "Get all payment providers", description = "Returns all providers with real-time health and circuit breaker status")
    public ResponseEntity<List<ProviderDto>> getAllProviders() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get provider by ID", description = "Returns details and health of a specific payment provider")
    public ResponseEntity<ProviderDto> getProviderById(@PathVariable UUID id) {
        return ResponseEntity.ok(providerService.getProviderById(id));
    }

    @PostMapping
    @Operation(summary = "Register new provider", description = "Adds a new simulated payment provider gateway to the engine")
    public ResponseEntity<ProviderDto> createProvider(@Valid @RequestBody ProviderCreateRequest request) {
        return new ResponseEntity<>(providerService.createProvider(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update provider configuration", description = "Updates fees, latency, priority, or simulation parameters")
    public ResponseEntity<ProviderDto> updateProvider(@PathVariable UUID id, @Valid @RequestBody ProviderUpdateRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(id, request));
    }

    @PatchMapping("/{id}/health")
    @Operation(summary = "Update provider health / circuit breaker", description = "Enables/disables a provider or manually trips/resets its circuit breaker")
    public ResponseEntity<ProviderDto> updateProviderHealth(@PathVariable UUID id, @RequestBody ProviderHealthUpdateDto request) {
        return ResponseEntity.ok(providerService.updateProviderHealth(id, request));
    }
}
