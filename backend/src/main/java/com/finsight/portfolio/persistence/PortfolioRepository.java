package com.finsight.portfolio.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, String> {
    Page<PortfolioEntity> findByUserIdAndArchivedFalse(String userId, Pageable pageable);
    Optional<PortfolioEntity> findByIdAndUserIdAndArchivedFalse(String id, String userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PortfolioEntity p where p.id = :portfolioId and p.user.id = :userId and p.archived = false")
    Optional<PortfolioEntity> findOwnedForUpdate(@Param("portfolioId") String portfolioId,
            @Param("userId") String userId);
}
