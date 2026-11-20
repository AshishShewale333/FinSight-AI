CREATE TABLE portfolio_analytics_snapshots (
    portfolio_id CHAR(36) NOT NULL,
    metrics_json JSON NOT NULL,
    calculated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (portfolio_id),
    CONSTRAINT fk_analytics_snapshot_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
