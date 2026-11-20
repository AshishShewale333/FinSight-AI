package com.finsight.news.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.news.application.FinancialNewsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/news")
public class FinancialNewsController {
    private final FinancialNewsService service;
    public FinancialNewsController(FinancialNewsService service){this.service=service;}
    @GetMapping
    public ApiResponse<Page<FinancialNewsResponse>> list(@RequestParam(required=false) String companyId,
            @RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            HttpServletRequest request){
        return ApiResponse.success("News retrieved",service.list(companyId,page,size),cid(request));
    }
    @GetMapping("/{newsId}")
    public ApiResponse<FinancialNewsResponse> get(@PathVariable String newsId,HttpServletRequest request){
        return ApiResponse.success("News item retrieved",service.get(newsId),cid(request));
    }
    @PostMapping("/{newsId}/summarize")
    public ResponseEntity<ApiResponse<FinancialNewsResponse>> summarize(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String newsId,HttpServletRequest request){
        return ResponseEntity.accepted().body(ApiResponse.success("News summarization queued",
                service.requestSummary(jwt.getSubject(),newsId),cid(request)));
    }
    private String cid(HttpServletRequest request){return request.getHeader(CorrelationIdFilter.HEADER_NAME);}
}
