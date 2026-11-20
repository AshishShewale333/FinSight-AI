CREATE TABLE portfolio_orders (
    id CHAR(36) NOT NULL,
    portfolio_id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    side VARCHAR(4) NOT NULL,
    order_type VARCHAR(8) NOT NULL,
    status VARCHAR(10) NOT NULL,
    quantity DECIMAL(24, 8) NOT NULL,
    limit_price DECIMAL(19, 4) NULL,
    executed_price DECIMAL(19, 4) NULL,
    fees DECIMAL(19, 4) NOT NULL DEFAULT 0,
    rejection_reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    filled_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_portfolio_orders_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT ck_portfolio_orders_type CHECK (order_type IN ('MARKET', 'LIMIT')),
    CONSTRAINT ck_portfolio_orders_status CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED', 'REJECTED')),
    CONSTRAINT ck_portfolio_orders_prices CHECK (
        (order_type = 'MARKET' AND limit_price IS NULL)
        OR (order_type = 'LIMIT' AND limit_price > 0)
    ),
    CONSTRAINT fk_portfolio_orders_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
    CONSTRAINT fk_portfolio_orders_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE RESTRICT,
    KEY ix_orders_pending_company (company_id, status, created_at),
    KEY ix_orders_portfolio_created (portfolio_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
