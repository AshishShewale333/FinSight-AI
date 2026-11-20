package com.finsight.portfolio.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.portfolio.application.PortfolioOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/orders")
public class PortfolioOrderController {
    private final PortfolioOrderService orders;

    public PortfolioOrderController(PortfolioOrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, HttpServletRequest request) {
        return ApiResponse.success("Orders retrieved", orders.list(jwt.getSubject(), portfolioId), cid(request));
    }

    @PostMapping
    public ApiResponse<OrderResponse> place(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, @Valid @RequestBody PlaceOrderRequest body,
            HttpServletRequest request) {
        return ApiResponse.success("Order placed", orders.place(jwt.getSubject(), portfolioId, body), cid(request));
    }

    @PutMapping("/{orderId}")
    public ApiResponse<OrderResponse> update(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, @PathVariable String orderId,
            @Valid @RequestBody PlaceOrderRequest body, HttpServletRequest request) {
        return ApiResponse.success("Pending order modified",
                orders.update(jwt.getSubject(), portfolioId, orderId, body), cid(request));
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<OrderResponse> cancel(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String portfolioId, @PathVariable String orderId, HttpServletRequest request) {
        return ApiResponse.success("Order cancelled", orders.cancel(jwt.getSubject(), portfolioId, orderId), cid(request));
    }

    private String cid(HttpServletRequest request) {
        return request.getHeader(CorrelationIdFilter.HEADER_NAME);
    }
}
