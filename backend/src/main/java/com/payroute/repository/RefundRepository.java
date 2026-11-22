package com.payroute.repository;

import com.payroute.entity.Merchant;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByTransactionOrderByCreatedAtDesc(PaymentTransaction transaction);
    Page<Refund> findByMerchantOrderByCreatedAtDesc(Merchant merchant, Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.transaction = :transaction AND r.status = 'SUCCESS'")
    BigDecimal sumSuccessfulRefundsByTransaction(@Param("transaction") PaymentTransaction transaction);

    @Query("SELECT COUNT(r) FROM Refund r")
    long countTotalRefunds();

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.status = 'SUCCESS'")
    BigDecimal sumTotalRefundAmount();
}
