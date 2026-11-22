package com.payroute.repository;

import com.payroute.entity.Merchant;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>, JpaSpecificationExecutor<PaymentTransaction> {
    
    Optional<PaymentTransaction> findByMerchantAndIdempotencyKey(Merchant merchant, String idempotencyKey);
    
    Page<PaymentTransaction> findByMerchantOrderByCreatedAtDesc(Merchant merchant, Pageable pageable);

    @Query("SELECT COUNT(t) FROM PaymentTransaction t")
    long countTotalTransactions();

    @Query("SELECT COUNT(t) FROM PaymentTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.status = 'SUCCESS'")
    BigDecimal sumSuccessfulVolume();

    @Query("SELECT COALESCE(SUM(t.feeCharged), 0) FROM PaymentTransaction t WHERE t.status = 'SUCCESS'")
    BigDecimal sumTotalFees();

    @Query("SELECT COUNT(t) FROM PaymentTransaction t WHERE t.merchant = :merchant")
    long countByMerchant(@Param("merchant") Merchant merchant);

    @Query("SELECT COUNT(t) FROM PaymentTransaction t WHERE t.merchant = :merchant AND t.status = :status")
    long countByMerchantAndStatus(@Param("merchant") Merchant merchant, @Param("status") TransactionStatus status);

    @Query("SELECT t.selectedProvider.code as providerCode, COUNT(t) as totalCount, " +
           "SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount, " +
           "SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END) as failedCount, " +
           "COALESCE(SUM(t.feeCharged), 0) as totalFee " +
           "FROM PaymentTransaction t " +
           "WHERE t.selectedProvider IS NOT NULL " +
           "GROUP BY t.selectedProvider.code")
    List<Object[]> getProviderAggregateStats();

    @Query("SELECT t FROM PaymentTransaction t WHERE t.createdAt >= :startDate ORDER BY t.createdAt ASC")
    List<PaymentTransaction> findTransactionsSince(@Param("startDate") LocalDateTime startDate);
}
