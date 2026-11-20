package com.finsight.aiassistant.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiResearchReportRepository extends JpaRepository<AiResearchReportEntity,String> {
    Optional<AiResearchReportEntity> findByIdAndOwnerId(String id,String ownerId);
    Optional<AiResearchReportEntity> findByOwnerIdAndIdempotencyKey(String ownerId,String key);
}
