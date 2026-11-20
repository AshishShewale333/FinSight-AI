package com.finsight.portfolio.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioOrderRepository extends JpaRepository<PortfolioOrderEntity, String> {
    List<PortfolioOrderEntity> findByPortfolioIdOrderByCreatedAtDesc(String portfolioId);

    Optional<PortfolioOrderEntity> findByIdAndPortfolioId(String id, String portfolioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PortfolioOrderEntity o where o.id = :orderId and o.portfolio.id = :portfolioId")
    Optional<PortfolioOrderEntity> findByIdAndPortfolioIdForUpdate(@Param("orderId") String orderId,
            @Param("portfolioId") String portfolioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PortfolioOrderEntity o join fetch o.portfolio p join fetch p.user " +
            "join fetch o.company where o.company.id = :companyId and o.status = 'PENDING' " +
            "order by o.createdAt asc")
    List<PortfolioOrderEntity> findPendingForCompanyForUpdate(@Param("companyId") String companyId);
}
