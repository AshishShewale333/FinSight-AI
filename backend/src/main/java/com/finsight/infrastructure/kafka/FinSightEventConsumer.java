package com.finsight.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.analytics.application.AnalyticsSnapshotService;
import com.finsight.news.application.FinancialNewsService;
import com.finsight.aiassistant.reports.ResearchReportService;
import com.finsight.notification.application.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class FinSightEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(FinSightEventConsumer.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AnalyticsSnapshotService analytics;
    private final NotificationService notifications;
    private final FinancialNewsService news;
    private final ResearchReportService reports;

    public FinSightEventConsumer(JdbcTemplate jdbc, ObjectMapper objectMapper,
                                 AnalyticsSnapshotService analytics, NotificationService notifications,
                                 FinancialNewsService news, ResearchReportService reports) {
        this.jdbc=jdbc; this.objectMapper=objectMapper; this.analytics=analytics;
        this.notifications=notifications; this.news=news; this.reports=reports;
    }

    @KafkaListener(topics={"portfolio-transaction-topic", "stock-price-update-topic",
            "financial-news-topic", "ai-report-request-topic", "notification-topic"},
            groupId="finsight-processing")
    @Transactional
    public void consume(String rawEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws Exception {
        JsonNode event = objectMapper.readTree(rawEvent);
        String eventId = requiredText(event, "eventId");
        String eventType = requiredText(event, "eventType");
        String aggregateId = requiredText(event, "aggregateId");
        if (!event.has("schemaVersion") || event.get("schemaVersion").asInt() != 1 || !event.hasNonNull("payload")) {
            throw new IllegalArgumentException("Unsupported or invalid event envelope");
        }
        int inserted = jdbc.update("INSERT IGNORE INTO processed_events (id, consumer_name, event_id, processed_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP(6))",
                UUID.randomUUID().toString(), "finsight-processing:" + topic, eventId);
        if (inserted == 0) {
            log.info("Skipping duplicate Kafka event eventId={} topic={}", eventId, topic);
            return;
        }
        JsonNode payload = event.get("payload");
        switch (eventType) {
            case "PortfolioTransactionRecorded" -> analytics.recalculate(requiredText(payload, "portfolioId"));
            case "StockQuoteUpdated" -> notifications.processQuote(requiredText(payload, "companyId"),
                    new BigDecimal(requiredText(payload, "price")), requiredText(payload, "currency"),
                    requiredText(payload, "status"), Instant.parse(requiredText(payload, "observedAt")));
            case "FinancialNewsSummaryRequested", "FinancialNewsIngested" -> news.processSummary(requiredText(payload,"newsId"));
            case "AiResearchRequested" -> reports.process(requiredText(payload,"reportId"));
            case "NotificationRequested" -> notifications.processNotificationRequest(
                    requiredText(payload,"userId"),requiredText(payload,"notificationType"),
                    requiredText(payload,"title"),requiredText(payload,"body"),requiredText(payload,"dedupeKey"));
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }

    @KafkaListener(topics={"portfolio-transaction-topic-dlt","stock-price-update-topic-dlt",
            "financial-news-topic-dlt","ai-report-request-topic-dlt","notification-dlt"},
            groupId="finsight-dlt-monitor")
    public void handleDeadLetter(String rawEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode event=objectMapper.readTree(rawEvent);
            String type=event.path("eventType").asText();
            JsonNode payload=event.path("payload");
            if ("AiResearchRequested".equals(type) && payload.hasNonNull("reportId")) {
                reports.markFailed(payload.get("reportId").asText(),"Report processing exhausted retries; inspect the AI report DLT");
            } else if (("FinancialNewsSummaryRequested".equals(type)||"FinancialNewsIngested".equals(type))
                    && payload.hasNonNull("newsId")) {
                news.markFailed(payload.get("newsId").asText());
            }
            log.error("Kafka event moved to dead letter topic topic={} eventId={} type={}",
                    topic,event.path("eventId").asText("unknown"),type);
        } catch (Exception exception) {
            log.error("Could not process dead letter record topic={}",topic,exception);
        }
    }

    private String requiredText(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required event field: " + field);
        }
        return value.asText();
    }
}
