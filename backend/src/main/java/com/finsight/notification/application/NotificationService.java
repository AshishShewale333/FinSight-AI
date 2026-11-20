package com.finsight.notification.application;

import com.finsight.common.error.ApiException;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.notification.api.CreatePriceAlertRequest;
import com.finsight.notification.api.NotificationResponse;
import com.finsight.notification.api.PriceAlertResponse;
import com.finsight.notification.persistence.NotificationEntity;
import com.finsight.notification.persistence.NotificationRepository;
import com.finsight.notification.persistence.PriceAlertEntity;
import com.finsight.notification.persistence.PriceAlertRepository;
import com.finsight.user.persistence.UserEntity;
import com.finsight.user.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final PriceAlertRepository alerts;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, PriceAlertRepository alerts,
                               UserRepository users, CompanyRepository companies, Clock clock) {
        this.notifications=notifications; this.alerts=alerts; this.users=users; this.companies=companies; this.clock=clock;
    }

    @Transactional(readOnly=true)
    public Page<NotificationResponse> list(String userId, int page, int size) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::notificationResponse);
    }

    @Transactional
    public NotificationResponse markRead(String userId, String notificationId) {
        NotificationEntity entity = notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> notFound("NOTIFICATION_NOT_FOUND", "Notification was not found"));
        entity.markRead(clock.instant());
        return notificationResponse(entity);
    }

    @Transactional(readOnly=true)
    public List<PriceAlertResponse> listAlerts(String userId) {
        return alerts.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::alertResponse).toList();
    }

    @Transactional
    public PriceAlertResponse createAlert(String userId, CreatePriceAlertRequest request) {
        UserEntity user = users.findById(userId).orElseThrow(() -> notFound("USER_NOT_FOUND", "User was not found"));
        CompanyEntity company = companies.findById(request.companyId())
                .orElseThrow(() -> notFound("COMPANY_NOT_FOUND", "Company was not found"));
        PriceAlertEntity alert = alerts.save(new PriceAlertEntity(UUID.randomUUID().toString(), user, company,
                request.comparison(), request.thresholdPrice(), company.getCurrency(), clock.instant()));
        return alertResponse(alert);
    }

    @Transactional
    public void deleteAlert(String userId, String alertId) {
        PriceAlertEntity alert = alerts.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> notFound("PRICE_ALERT_NOT_FOUND", "Price alert was not found"));
        alerts.delete(alert);
    }

    @Transactional
    public void processQuote(String companyId, BigDecimal price, String currency,
                             String quoteStatus, Instant observedAt) {
        if ("MOCK".equals(quoteStatus)) return;
        for (PriceAlertEntity alert : alerts.findByCompanyIdAndEnabledTrue(companyId)) {
            if (alert.getCurrency().equals(currency) && alert.matches(price)) {
                alert.trigger(observedAt);
                String body = alert.getCompany().getSymbol() + " reached " + price + " " + currency
                        + " (" + alert.getComparison().toLowerCase() + " " + alert.getThresholdPrice() + ")";
                notifications.save(new NotificationEntity(UUID.randomUUID().toString(), alert.getUser(),
                        "PRICE_ALERT", "Price alert for " + alert.getCompany().getSymbol(), body,
                        "price-alert:" + alert.getId() + ":" + observedAt.toEpochMilli(), observedAt));
            }
        }
    }

    @Transactional
    public void processNotificationRequest(String userId, String eventType, String title, String body, String dedupeKey) {
        if (notifications.findByUserIdAndDedupeKey(userId, dedupeKey).isPresent()) return;
        UserEntity user=users.findById(userId).orElseThrow(()->new IllegalArgumentException("Notification user does not exist"));
        notifications.save(new NotificationEntity(UUID.randomUUID().toString(),user,eventType,title,body,dedupeKey,clock.instant()));
    }

    private NotificationResponse notificationResponse(NotificationEntity entity) {
        return new NotificationResponse(entity.getId(), entity.getEventType(), entity.getTitle(),
                entity.getBody(), entity.getCreatedAt(), entity.getReadAt());
    }
    private PriceAlertResponse alertResponse(PriceAlertEntity entity) {
        return new PriceAlertResponse(entity.getId(), entity.getCompany().getId(), entity.getCompany().getSymbol(),
                entity.getComparison(), entity.getThresholdPrice(), entity.getCurrency(), entity.isEnabled(),
                entity.getLastTriggeredAt(), entity.getCreatedAt());
    }
    private ApiException notFound(String code, String message) { return new ApiException(HttpStatus.NOT_FOUND, code, message); }
}
