package com.payroute.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroute.entity.IdempotencyRecord;
import com.payroute.entity.IdempotencyStatus;
import com.payroute.entity.Merchant;
import com.payroute.exception.IdempotencyConflictException;
import com.payroute.exception.IdempotencyPayloadMismatchException;
import com.payroute.repository.IdempotencyRecordRepository;
import com.payroute.security.ApiKeyAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * Attempts to acquire an idempotency lock for this merchant and key.
     * Returns cached record if previously completed with matching payload.
     * Throws conflict exception if in progress, or mismatch exception if payload altered.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> acquireOrCheck(Merchant merchant, String idempotencyKey, Object payload) {
        String payloadJson = serializePayload(payload);
        String requestHash = ApiKeyAuthenticationFilter.hashSha256(payloadJson);

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByMerchantAndIdempotencyKey(merchant, idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                // Check if stale lock (older than 2 minutes)
                if (record.getLockedAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
                    log.warn("Clearing stale in-progress idempotency lock for key: {}", idempotencyKey);
                    record.setLockedAt(LocalDateTime.now());
                    return Optional.empty();
                }
                throw new IdempotencyConflictException(
                        "A concurrent transaction is currently in progress for Idempotency-Key: " + idempotencyKey);
            }

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyPayloadMismatchException(
                            "Idempotency-Key '" + idempotencyKey + "' was already executed with a different request payload.");
                }
                log.info("Idempotency HIT for merchant: {}, key: {}", merchant.getMerchantCode(), idempotencyKey);
                return Optional.of(record);
            }
        }

        // Try to insert a new IN_PROGRESS record
        try {
            IdempotencyRecord newRecord = IdempotencyRecord.builder()
                    .merchant(merchant)
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .status(IdempotencyStatus.IN_PROGRESS)
                    .lockedAt(LocalDateTime.now())
                    .build();

            idempotencyRecordRepository.saveAndFlush(newRecord);
            return Optional.empty(); // Acquired lock, proceed with execution
        } catch (DataIntegrityViolationException ex) {
            // Concurrent race condition caught by database unique constraint
            log.warn("Concurrent duplicate request detected by DB unique constraint for key: {}", idempotencyKey);
            throw new IdempotencyConflictException(
                    "Concurrent duplicate request detected for Idempotency-Key: " + idempotencyKey);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCompletion(Merchant merchant, String idempotencyKey, String responsePayload, int responseCode) {
        idempotencyRecordRepository.findByMerchantAndIdempotencyKey(merchant, idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponsePayload(responsePayload);
            record.setResponseCode(responseCode);
            idempotencyRecordRepository.save(record);
            log.info("Completed idempotency record for key: {}", idempotencyKey);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Merchant merchant, String idempotencyKey, String errorPayload, int responseCode) {
        idempotencyRecordRepository.findByMerchantAndIdempotencyKey(merchant, idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponsePayload(errorPayload);
            record.setResponseCode(responseCode);
            idempotencyRecordRepository.save(record);
        });
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }
}
