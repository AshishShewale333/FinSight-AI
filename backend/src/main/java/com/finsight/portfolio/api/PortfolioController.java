package com.finsight.portfolio.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.portfolio.application.PortfolioService;
import com.finsight.transaction.api.CreateTransactionRequest;
import com.finsight.transaction.api.TransactionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {
    private final PortfolioService portfolioService;
    public PortfolioController(PortfolioService portfolioService) { this.portfolioService = portfolioService; }

    @GetMapping
    public ApiResponse<Page<PortfolioResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size, HttpServletRequest request) {
        return ApiResponse.success("Portfolios retrieved", portfolioService.list(jwt.getSubject(), page, size), cid(request));
    }

    @PostMapping
    public ApiResponse<PortfolioResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePortfolioRequest body, HttpServletRequest request) {
        return ApiResponse.success("Portfolio created", portfolioService.create(jwt.getSubject(), body), cid(request));
    }

    @GetMapping("/{portfolioId}")
    public ApiResponse<PortfolioResponse> get(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, HttpServletRequest request) {
        return ApiResponse.success("Portfolio retrieved", portfolioService.get(jwt.getSubject(), portfolioId), cid(request));
    }

    @DeleteMapping("/{portfolioId}")
    public ApiResponse<Void> archive(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, HttpServletRequest request) {
        portfolioService.archive(jwt.getSubject(), portfolioId);
        return ApiResponse.success("Portfolio archived", null, cid(request));
    }

    @GetMapping("/{portfolioId}/holdings")
    public ApiResponse<List<HoldingResponse>> holdings(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, HttpServletRequest request) {
        return ApiResponse.success("Holdings retrieved", portfolioService.holdings(jwt.getSubject(), portfolioId), cid(request));
    }

    @PostMapping("/{portfolioId}/transactions")
    public ApiResponse<TransactionResponse> addTransaction(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, @Valid @RequestBody CreateTransactionRequest body,
            @RequestHeader(value="Idempotency-Key", required=false) String key, HttpServletRequest request) {
        return ApiResponse.success("Transaction recorded",
                portfolioService.addTransaction(jwt.getSubject(), portfolioId, body, key), cid(request));
    }

    @GetMapping("/{portfolioId}/transactions")
    public ApiResponse<Page<TransactionResponse>> transactions(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int size, HttpServletRequest request) {
        return ApiResponse.success("Transactions retrieved",
                portfolioService.transactions(jwt.getSubject(), portfolioId, page, size), cid(request));
    }

    private String cid(HttpServletRequest request) { return request.getHeader(CorrelationIdFilter.HEADER_NAME); }
}
