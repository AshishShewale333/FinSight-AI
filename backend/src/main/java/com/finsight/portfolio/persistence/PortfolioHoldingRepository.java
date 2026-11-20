package com.finsight.portfolio.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHoldingEntity, String> {
    List<PortfolioHoldingEntity> findByPortfolioId(String portfolioId);
    Optional<PortfolioHoldingEntity> findByPortfolioIdAndCompanyId(String portfolioId, String companyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from PortfolioHoldingEntity h where h.portfolio.id = :portfolioId and h.company.id = :companyId")
    Optional<PortfolioHoldingEntity> findByPortfolioIdAndCompanyIdForUpdate(
            @Param("portfolioId") String portfolioId, @Param("companyId") String companyId);
}
