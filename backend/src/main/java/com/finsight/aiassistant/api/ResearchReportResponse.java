package com.finsight.aiassistant.api;

import java.time.Instant;
import java.util.Map;

public record ResearchReportResponse(String id,String companyId,String companySymbol,String status,
        String promptVersion,String modelName,Map<String,Object> result,String failureReason,
        Instant createdAt,Instant completedAt) { }
