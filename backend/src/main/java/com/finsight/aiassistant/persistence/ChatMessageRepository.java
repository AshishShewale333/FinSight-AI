package com.finsight.aiassistant.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {
    Page<ChatMessageEntity> findBySessionIdAndSessionUserIdOrderBySequenceNoAsc(String sessionId, String userId, Pageable pageable);
    @Query("select coalesce(max(message.sequenceNo), 0) from ChatMessageEntity message where message.session.id = :sessionId")
    long findLastSequence(@Param("sessionId") String sessionId);
    Optional<ChatMessageEntity> findTopBySessionIdOrderBySequenceNoDesc(String sessionId);
}
