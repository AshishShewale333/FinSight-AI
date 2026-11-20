package com.finsight.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface PriceAlertRepository extends JpaRepository<PriceAlertEntity, String> {
    List<PriceAlertEntity> findByCompanyIdAndEnabledTrue(String companyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PriceAlertEntity a join fetch a.user join fetch a.company " +
            "where a.company.id = :companyId and a.enabled = true")
    List<PriceAlertEntity> findActiveForCompanyForUpdate(@Param("companyId") String companyId);
    List<PriceAlertEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PriceAlertEntity> findByIdAndUserId(String id, String userId);
}
