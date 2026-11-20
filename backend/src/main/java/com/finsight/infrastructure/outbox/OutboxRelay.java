package com.finsight.infrastructure.outbox;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka) {
        this.jdbc=jdbc; this.kafka=kafka;
    }

    @Scheduled(fixedDelayString = "${OUTBOX_POLL_INTERVAL:3000}")
    @Transactional
    public void publishPending() {
        List<PendingEvent> events = jdbc.query("SELECT id, aggregate_id, event_type, payload FROM outbox_events WHERE published_at IS NULL ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED",
                (rs, row) -> new PendingEvent(rs.getString("id"), rs.getString("aggregate_id"),
                        rs.getString("event_type"), rs.getString("payload")));
        for (PendingEvent event : events) {
            String topic = topicFor(event.eventType());
            try {
                kafka.send(topic, event.aggregateId(), event.payload()).get(5, TimeUnit.SECONDS);
                jdbc.update("UPDATE outbox_events SET published_at = CURRENT_TIMESTAMP(6), attempts = attempts + 1 WHERE id = ?", event.id());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                jdbc.update("UPDATE outbox_events SET attempts = attempts + 1 WHERE id = ?", event.id());
                log.warn("Outbox publish failed eventId={} topic={} attempt remains pending", event.id(), topic, exception);
                break;
            }
        }
    }

    private String topicFor(String eventType) {
        return switch (eventType) {
            case "PortfolioTransactionRecorded" -> "portfolio-transaction-topic";
            case "StockQuoteUpdated" -> "stock-price-update-topic";
            case "FinancialNewsIngested", "FinancialNewsSummaryRequested" -> "financial-news-topic";
            case "AiResearchRequested" -> "ai-report-request-topic";
            case "NotificationRequested" -> "notification-topic";
            default -> throw new IllegalArgumentException("No Kafka route for outbox event type: " + eventType);
        };
    }

    private record PendingEvent(String id, String aggregateId, String eventType, String payload) { }
}
