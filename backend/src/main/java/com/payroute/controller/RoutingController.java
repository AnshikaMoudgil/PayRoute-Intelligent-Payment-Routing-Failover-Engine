package com.payroute.controller;

import com.payroute.dto.RoutingRuleCreateRequest;
import com.payroute.dto.RoutingRuleDto;
import com.payroute.dto.RoutingRuleUpdateRequest;
import com.payroute.service.RoutingRuleService;
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
@RequestMapping("/api/routing/rules")
@RequiredArgsConstructor
@Tag(name = "Routing Configuration", description = "Endpoints for configuring routing strategies, priority, weights, and merchant overrides")
public class RoutingController {

    private final RoutingRuleService routingRuleService;

    @GetMapping
    @Operation(summary = "Get all routing rules", description = "Lists global and merchant-specific routing rules")
    public ResponseEntity<List<RoutingRuleDto>> getAllRules() {
        return ResponseEntity.ok(routingRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get routing rule by ID", description = "Retrieves details of a specific routing rule")
    public ResponseEntity<RoutingRuleDto> getRuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(routingRuleService.getRuleById(id));
    }

    @PostMapping
    @Operation(summary = "Create routing rule", description = "Adds a new routing strategy configuration for a merchant or global scope")
    public ResponseEntity<RoutingRuleDto> createRule(@Valid @RequestBody RoutingRuleCreateRequest request) {
        return new ResponseEntity<>(routingRuleService.createRule(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update routing rule", description = "Updates strategy, amount ranges, weights, or active status")
    public ResponseEntity<RoutingRuleDto> updateRule(@PathVariable UUID id, @Valid @RequestBody RoutingRuleUpdateRequest request) {
        return ResponseEntity.ok(routingRuleService.updateRule(id, request));
    }
}
