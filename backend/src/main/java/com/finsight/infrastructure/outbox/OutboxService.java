package com.finsight.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxService(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc=jdbc; this.objectMapper=objectMapper; this.clock=clock;
    }

    public String record(String aggregateType, String aggregateId, String eventType, Object payload) {
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) correlationId = eventId;
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("schemaVersion", 1);
        envelope.put("occurredAt", clock.instant());
        envelope.put("correlationId", correlationId);
        envelope.put("aggregateId", aggregateId);
        envelope.put("payload", payload);
        try {
            jdbc.update("INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, schema_version, payload, correlation_id, created_at) VALUES (?, ?, ?, ?, 1, ?, ?, ?)",
                    eventId, aggregateType, aggregateId, eventType,
                    objectMapper.writeValueAsString(envelope), correlationId, clock.instant());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Event payload could not be serialized", exception);
        }
        return eventId;
    }
}
