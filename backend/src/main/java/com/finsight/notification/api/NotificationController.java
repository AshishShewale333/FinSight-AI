package com.finsight.notification.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.notification.application.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service=service; }
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size, HttpServletRequest request) {
        return ApiResponse.success("Notifications retrieved", service.list(jwt.getSubject(), page, size), cid(request));
    }
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String notificationId, HttpServletRequest request) {
        return ApiResponse.success("Notification marked read", service.markRead(jwt.getSubject(), notificationId), cid(request));
    }
    private String cid(HttpServletRequest request) { return request.getHeader(CorrelationIdFilter.HEADER_NAME); }
}
