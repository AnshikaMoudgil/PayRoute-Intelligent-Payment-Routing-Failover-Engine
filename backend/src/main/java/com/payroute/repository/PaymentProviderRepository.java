package com.payroute.repository;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, UUID> {
    Optional<PaymentProvider> findByCode(String code);
    List<PaymentProvider> findByStatus(ProviderStatus status);
    List<PaymentProvider> findByStatusOrderByPriorityAsc(ProviderStatus status);
}
