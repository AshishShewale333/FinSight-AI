package com.finsight.notification.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.beans.factory.annotation.Value;

@Service
@ConditionalOnProperty(prefix = "finsight.notifications.email", name = "enabled", havingValue = "true")
public class EmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private final JavaMailSender mailSender;
    private final String from;
    private final String recipient;

    public EmailNotificationService(JavaMailSender mailSender,
            @Value("${finsight.notifications.email.from:finsight@localhost}") String from,
            @Value("${finsight.notifications.email.recipient:ashishshewale0306@gmail.com}") String recipient,
            @Value("${spring.mail.password:}") String smtpPassword) {
        this.mailSender = mailSender;
        this.from = from;
        this.recipient = recipient;
        if (smtpPassword == null || smtpPassword.isBlank()) {
            log.warn("Price alert email is enabled but SMTP password is not configured");
        } else {
            log.info("Price alert email delivery is enabled");
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendPriceAlert(PriceAlertTriggeredEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("FinSight price alert: " + event.symbol());
        message.setText("Your " + event.symbol() + " price alert was triggered.\n\n"
                + "Condition: price crossed " + event.comparison().toLowerCase() + " "
                + event.thresholdPrice().toPlainString() + " " + event.currency() + ".\n"
                + "Observed price: " + event.currentPrice().toPlainString() + " " + event.currency() + ".\n"
                + "Time: " + event.triggeredAt() + "\n\n"
                + "This is a locally generated mock price and is not a live market quote.");
        try {
            mailSender.send(message);
            log.info("Price alert email sent alertId={} triggerCount={}",
                    event.alertId(), event.triggerCount());
        } catch (RuntimeException exception) {
            log.warn("Price alert email delivery failed alertId={} triggerCount={}",
                    event.alertId(), event.triggerCount(), exception);
        }
    }
}
