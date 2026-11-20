package com.finsight.marketdata.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.marketdata.application.MarketDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/quotes")
public class MarketDataController {
    private final MarketDataService marketDataService;
    public MarketDataController(MarketDataService marketDataService) { this.marketDataService = marketDataService; }

    @GetMapping("/latest")
    public ApiResponse<QuoteResponse> latest(@PathVariable String companyId, HttpServletRequest request) {
        return ApiResponse.success("Quote retrieved", marketDataService.latestQuote(companyId),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    @GetMapping("/stored")
    public ApiResponse<QuoteResponse> stored(@PathVariable String companyId, HttpServletRequest request) {
        return ApiResponse.success("Stored quote retrieved", marketDataService.lastStoredQuote(companyId),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
