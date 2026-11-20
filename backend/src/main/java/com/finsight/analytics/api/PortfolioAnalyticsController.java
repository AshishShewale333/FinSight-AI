package com.finsight.analytics.api;

import com.finsight.analytics.application.PortfolioAnalyticsService;
import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/analytics")
public class PortfolioAnalyticsController {
    private final PortfolioAnalyticsService analytics;
    public PortfolioAnalyticsController(PortfolioAnalyticsService analytics) { this.analytics=analytics; }

    @GetMapping
    public ApiResponse<PortfolioAnalyticsResponse> get(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, HttpServletRequest request) {
        return ApiResponse.success("Portfolio analytics calculated",
                analytics.calculate(jwt.getSubject(), portfolioId),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
