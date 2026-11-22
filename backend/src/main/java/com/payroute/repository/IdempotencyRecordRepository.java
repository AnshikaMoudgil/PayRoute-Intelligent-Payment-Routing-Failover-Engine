package com.payroute.repository;

import com.payroute.entity.IdempotencyRecord;
import com.payroute.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByMerchantAndIdempotencyKey(Merchant merchant, String idempotencyKey);
}
