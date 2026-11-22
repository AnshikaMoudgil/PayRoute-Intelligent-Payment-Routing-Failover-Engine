package com.payroute.repository;

import com.payroute.entity.AttemptStatus;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.ProviderAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderAttemptRepository extends JpaRepository<ProviderAttempt, UUID> {
    List<ProviderAttempt> findByTransactionOrderByAttemptNumberAsc(PaymentTransaction transaction);
    
    @Query("SELECT COUNT(a) FROM ProviderAttempt a WHERE a.isFailover = true")
    long countTotalFailovers();

    @Query("SELECT a.provider.code as providerCode, AVG(a.latencyMs) as avgLatency " +
           "FROM ProviderAttempt a " +
           "GROUP BY a.provider.code")
    List<Object[]> getProviderAverageLatencies();

    @Query("SELECT a.provider.code as providerCode, COUNT(a) as totalAttempts, " +
           "SUM(CASE WHEN a.status = 'SUCCESS' THEN 1 ELSE 0 END) as successAttempts " +
           "FROM ProviderAttempt a " +
           "GROUP BY a.provider.code")
    List<Object[]> getProviderAttemptStats();
}
