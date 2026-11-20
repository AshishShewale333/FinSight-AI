package com.finsight.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHoldingEntity, String> {
    List<PortfolioHoldingEntity> findByPortfolioId(String portfolioId);
    Optional<PortfolioHoldingEntity> findByPortfolioIdAndCompanyId(String portfolioId, String companyId);
}
