package com.payroute.repository;

import com.payroute.entity.Merchant;
import com.payroute.entity.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {
    List<RoutingRule> findByMerchantAndIsActiveTrue(Merchant merchant);
    List<RoutingRule> findByMerchantIsNullAndIsActiveTrue();
    Optional<RoutingRule> findFirstByMerchantAndIsActiveTrueOrderByCreatedAtDesc(Merchant merchant);
    Optional<RoutingRule> findFirstByMerchantIsNullAndIsActiveTrueOrderByCreatedAtDesc();
}
