package com.payroute.controller;

import com.payroute.dto.MerchantCreateRequest;
import com.payroute.dto.MerchantResponse;
import com.payroute.service.MerchantService;
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
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "Endpoints for merchant registration, credentials and preferences")
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    @Operation(summary = "Register a new merchant", description = "Creates a merchant and generates an initial API key")
    public ResponseEntity<MerchantResponse> registerMerchant(@Valid @RequestBody MerchantCreateRequest request) {
        MerchantResponse response = merchantService.registerMerchant(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all merchants", description = "Retrieves a list of all registered merchants")
    public ResponseEntity<List<MerchantResponse>> getAllMerchants() {
        return ResponseEntity.ok(merchantService.getAllMerchants());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get merchant by ID", description = "Retrieves details of a specific merchant by their UUID")
    public ResponseEntity<MerchantResponse> getMerchantById(@PathVariable UUID id) {
        return ResponseEntity.ok(merchantService.getMerchantById(id));
    }
}
