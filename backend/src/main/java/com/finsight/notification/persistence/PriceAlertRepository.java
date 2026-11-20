package com.finsight.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlertEntity, String> {
    List<PriceAlertEntity> findByCompanyIdAndEnabledTrue(String companyId);
    List<PriceAlertEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PriceAlertEntity> findByIdAndUserId(String id, String userId);
}
