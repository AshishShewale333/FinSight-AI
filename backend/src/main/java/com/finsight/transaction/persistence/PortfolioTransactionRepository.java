package com.finsight.transaction.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransactionEntity, String> {
    Page<PortfolioTransactionEntity> findByPortfolioIdOrderByTradedAtDesc(String portfolioId, Pageable pageable);
    java.util.List<PortfolioTransactionEntity> findByPortfolioIdOrderByTradedAtDesc(String portfolioId);
    Optional<PortfolioTransactionEntity> findByPortfolioIdAndIdempotencyKey(String portfolioId, String idempotencyKey);
}
