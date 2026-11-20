package com.finsight.analytics.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AnalyticsSnapshotService {
    private final JdbcTemplate jdbc;
    private final PortfolioAnalyticsService analytics;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AnalyticsSnapshotService(JdbcTemplate jdbc, PortfolioAnalyticsService analytics,
                                    ObjectMapper objectMapper, Clock clock) {
        this.jdbc=jdbc; this.analytics=analytics; this.objectMapper=objectMapper; this.clock=clock;
    }

    @Transactional
    public void recalculate(String portfolioId) {
        String userId = jdbc.query("SELECT user_id FROM portfolios WHERE id = ? AND archived = FALSE",
                rs -> rs.next() ? rs.getString(1) : null, portfolioId);
        if (userId == null) return;
        try {
            String json = objectMapper.writeValueAsString(analytics.calculate(userId, portfolioId));
            jdbc.update("INSERT INTO portfolio_analytics_snapshots (portfolio_id, metrics_json, calculated_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE metrics_json = VALUES(metrics_json), calculated_at = VALUES(calculated_at)",
                    portfolioId, json, clock.instant());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Portfolio analytics could not be serialized", exception);
        }
    }
}
