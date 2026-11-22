package com.payroute.controller;

import com.payroute.dto.RefundCreateRequest;
import com.payroute.dto.RefundResponse;
import com.payroute.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@Tag(name = "Refund Management", description = "Endpoints for initiating full and partial refunds and viewing history")
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @Operation(summary = "Initiate refund", description = "Issues a partial or full refund for an existing successful payment transaction")
    public ResponseEntity<RefundResponse> initiateRefund(@Valid @RequestBody RefundCreateRequest request) {
        return new ResponseEntity<>(refundService.initiateRefund(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get refund by ID", description = "Retrieves details of a specific refund record")
    public ResponseEntity<RefundResponse> getRefundById(@PathVariable UUID id) {
        return ResponseEntity.ok(refundService.getRefundById(id));
    }

    @GetMapping
    @Operation(summary = "List all refunds", description = "Paginated list of refunds ordered by creation date descending")
    public ResponseEntity<Page<RefundResponse>> getAllRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(refundService.getAllRefunds(pageRequest));
    }
}
