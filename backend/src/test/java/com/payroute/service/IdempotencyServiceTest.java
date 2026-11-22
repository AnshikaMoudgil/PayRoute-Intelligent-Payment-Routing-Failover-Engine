package com.payroute.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroute.entity.IdempotencyRecord;
import com.payroute.entity.IdempotencyStatus;
import com.payroute.entity.Merchant;
import com.payroute.exception.IdempotencyConflictException;
import com.payroute.exception.IdempotencyPayloadMismatchException;
import com.payroute.repository.IdempotencyRecordRepository;
import com.payroute.security.ApiKeyAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IdempotencyService idempotencyService;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(repository, objectMapper);
        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .merchantCode("MERCH-TEST")
                .build();
    }

    @Test
    @DisplayName("Acquire new idempotency key saves IN_PROGRESS record")
    void testAcquireNewKey() {
        when(repository.findByMerchantAndIdempotencyKey(any(), anyString()))
                .thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of("amount", 1000, "currency", "INR");
        Optional<IdempotencyRecord> result = idempotencyService.acquireOrCheck(merchant, "key_123", payload);

        assertTrue(result.isEmpty());
        verify(repository, times(1)).saveAndFlush(any(IdempotencyRecord.class));
    }

    @Test
    @DisplayName("Duplicate request with same payload returns completed record (HIT)")
    void testDuplicateHit() throws JsonProcessingException {
        Map<String, Object> payload = Map.of("amount", 1000, "currency", "INR");
        String json = objectMapper.writeValueAsString(payload);
        String hash = ApiKeyAuthenticationFilter.hashSha256(json);

        IdempotencyRecord completed = IdempotencyRecord.builder()
                .idempotencyKey("key_123")
                .merchant(merchant)
                .status(IdempotencyStatus.COMPLETED)
                .requestHash(hash)
                .responsePayload("{\"status\":\"SUCCESS\"}")
                .responseCode(200)
                .build();

        when(repository.findByMerchantAndIdempotencyKey(merchant, "key_123"))
                .thenReturn(Optional.of(completed));

        Optional<IdempotencyRecord> result = idempotencyService.acquireOrCheck(merchant, "key_123", payload);

        assertTrue(result.isPresent());
        assertEquals("{\"status\":\"SUCCESS\"}", result.get().getResponsePayload());
    }

    @Test
    @DisplayName("Duplicate request with altered payload throws IdempotencyPayloadMismatchException")
    void testPayloadMismatch() {
        Map<String, Object> newPayload = Map.of("amount", 9999, "currency", "INR");
        String oldHash = "different_old_hash_value";

        IdempotencyRecord completed = IdempotencyRecord.builder()
                .idempotencyKey("key_123")
                .merchant(merchant)
                .status(IdempotencyStatus.COMPLETED)
                .requestHash(oldHash)
                .build();

        when(repository.findByMerchantAndIdempotencyKey(merchant, "key_123"))
                .thenReturn(Optional.of(completed));

        assertThrows(IdempotencyPayloadMismatchException.class, () ->
                idempotencyService.acquireOrCheck(merchant, "key_123", newPayload));
    }

    @Test
    @DisplayName("Concurrent request while another is IN_PROGRESS throws IdempotencyConflictException")
    void testConcurrentInProgressConflict() {
        Map<String, Object> payload = Map.of("amount", 1000, "currency", "INR");

        IdempotencyRecord inProgress = IdempotencyRecord.builder()
                .idempotencyKey("key_123")
                .merchant(merchant)
                .status(IdempotencyStatus.IN_PROGRESS)
                .lockedAt(LocalDateTime.now())
                .build();

        when(repository.findByMerchantAndIdempotencyKey(merchant, "key_123"))
                .thenReturn(Optional.of(inProgress));

        assertThrows(IdempotencyConflictException.class, () ->
                idempotencyService.acquireOrCheck(merchant, "key_123", payload));
    }
}
