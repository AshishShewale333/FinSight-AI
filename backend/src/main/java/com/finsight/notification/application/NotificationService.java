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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notifications;
    private final PriceAlertRepository alerts;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository notifications, PriceAlertRepository alerts,
                               UserRepository users, CompanyRepository companies, Clock clock,
                               ApplicationEventPublisher events) {
        this.notifications=notifications; this.alerts=alerts; this.users=users; this.companies=companies;
        this.clock=clock; this.events=events;
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
        log.info("Price alert created alertId={} symbol={} comparison={}", alert.getId(),
                company.getSymbol(), request.comparison());
        return alertResponse(alert);
    }

    @Transactional
    public void deleteAlert(String userId, String alertId) {
        PriceAlertEntity alert = alerts.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> notFound("PRICE_ALERT_NOT_FOUND", "Price alert was not found"));
        alerts.delete(alert);
        log.info("Price alert deleted alertId={} symbol={}", alert.getId(), alert.getCompany().getSymbol());
    }

    @Transactional
    public PriceAlertResponse reactivateAlert(String userId, String alertId) {
        PriceAlertEntity alert = alerts.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> notFound("PRICE_ALERT_NOT_FOUND", "Price alert was not found"));
        alert.reactivate();
        log.info("Price alert reactivated alertId={} symbol={}", alert.getId(), alert.getCompany().getSymbol());
        return alertResponse(alert);
    }

    @Transactional
    public void processPriceChange(String companyId, BigDecimal previousPrice, BigDecimal currentPrice,
                                   String currency, String quoteStatus, Instant observedAt) {
        if ("MOCK".equals(quoteStatus) && previousPrice.compareTo(currentPrice) == 0) return;
        for (PriceAlertEntity alert : alerts.findActiveForCompanyForUpdate(companyId)) {
            if (!alert.getCurrency().equals(currency)
                    || !alert.crossed(previousPrice, currentPrice)) continue;
            alert.trigger(observedAt);
            log.info("Price alert triggered alertId={} triggerCount={} symbol={} quoteStatus={}",
                    alert.getId(), alert.getTriggerCount(), alert.getCompany().getSymbol(), quoteStatus);
            String body = alert.getCompany().getSymbol() + " crossed " + alert.getComparison().toLowerCase()
                    + " " + alert.getThresholdPrice() + " " + currency + "; observed price " + currentPrice;
            String dedupeKey = "price-alert:" + alert.getId() + ":" + alert.getTriggerCount();
            notifications.save(new NotificationEntity(UUID.randomUUID().toString(), alert.getUser(),
                    "PRICE_ALERT", "Price alert for " + alert.getCompany().getSymbol(), body,
                    dedupeKey, observedAt));
            events.publishEvent(new PriceAlertTriggeredEvent(alert.getId(), alert.getTriggerCount(),
                    alert.getUser().getEmail(), alert.getCompany().getSymbol(), alert.getComparison(),
                    alert.getThresholdPrice(), currentPrice, currency, observedAt));
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
