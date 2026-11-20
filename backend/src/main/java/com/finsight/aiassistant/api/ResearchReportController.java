package com.finsight.aiassistant.api;

import com.finsight.aiassistant.reports.ResearchReportService;
import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/reports")
public class ResearchReportController {
    private final ResearchReportService service;
    public ResearchReportController(ResearchReportService service){this.service=service;}
    @PostMapping
    public ResponseEntity<ApiResponse<ResearchReportResponse>> request(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ResearchReportRequest body,
            @RequestHeader(value="Idempotency-Key",required=false) String key,HttpServletRequest request){
        return ResponseEntity.accepted().body(ApiResponse.success("Research report queued",
                service.request(jwt.getSubject(),body.companyId(),key),request.getHeader(CorrelationIdFilter.HEADER_NAME)));
    }
    @GetMapping("/{reportId}")
    public ApiResponse<ResearchReportResponse> get(@AuthenticationPrincipal Jwt jwt,@PathVariable String reportId,
            HttpServletRequest request){
        return ApiResponse.success("Research report retrieved",service.get(jwt.getSubject(),reportId),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
