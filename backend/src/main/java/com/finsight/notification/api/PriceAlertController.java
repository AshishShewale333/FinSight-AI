package com.finsight.notification.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.notification.application.NotificationService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/alerts")
public class PriceAlertController {
    private final NotificationService service;
    public PriceAlertController(NotificationService service) { this.service=service; }

    @GetMapping
    public ApiResponse<List<PriceAlertResponse>> list(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return ApiResponse.success("Price alerts retrieved", service.listAlerts(jwt.getSubject()), cid(request));
    }
    @PostMapping
    public ApiResponse<PriceAlertResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePriceAlertRequest body, HttpServletRequest request) {
        return ApiResponse.success("Price alert created", service.createAlert(jwt.getSubject(), body), cid(request));
    }
    @DeleteMapping("/{alertId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String alertId,
                                    HttpServletRequest request) {
        service.deleteAlert(jwt.getSubject(), alertId);
        return ApiResponse.success("Price alert deleted", null, cid(request));
    }
    @PostMapping("/{alertId}/reactivate")
    public ApiResponse<PriceAlertResponse> reactivate(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String alertId, HttpServletRequest request) {
        return ApiResponse.success("Price alert reactivated", service.reactivateAlert(jwt.getSubject(), alertId), cid(request));
    }
    private String cid(HttpServletRequest request) { return request.getHeader(CorrelationIdFilter.HEADER_NAME); }
}
