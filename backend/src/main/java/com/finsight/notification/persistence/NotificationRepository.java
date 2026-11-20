package com.finsight.notification.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<NotificationEntity> findByIdAndUserId(String id, String userId);
    Optional<NotificationEntity> findByUserIdAndDedupeKey(String userId, String dedupeKey);
}
