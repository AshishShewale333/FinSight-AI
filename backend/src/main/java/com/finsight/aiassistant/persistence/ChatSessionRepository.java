package com.finsight.aiassistant.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, String> {
    Page<ChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);
    Optional<ChatSessionEntity> findByIdAndUserId(String id, String userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ChatSessionEntity session where session.id = :id and session.user.id = :userId")
    Optional<ChatSessionEntity> findForUpdate(@Param("id") String id, @Param("userId") String userId);
}
