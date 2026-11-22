package com.payroute.repository;

import com.payroute.entity.PaymentEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    List<PaymentEvent> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);
    List<PaymentEvent> findByPublishedToKafkaFalseOrderByCreatedAtAsc();
    Page<PaymentEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
