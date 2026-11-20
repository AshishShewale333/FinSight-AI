package com.finsight.portfolio.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, String> {
    Page<PortfolioEntity> findByUserIdAndArchivedFalse(String userId, Pageable pageable);
    Optional<PortfolioEntity> findByIdAndUserIdAndArchivedFalse(String id, String userId);
}
