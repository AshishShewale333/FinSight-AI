ALTER TABLE companies ADD COLUMN sector VARCHAR(120) NULL;
ALTER TABLE companies ADD COLUMN description TEXT NULL;

CREATE TABLE stock_quotes (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    price DECIMAL(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    quote_status VARCHAR(16) NOT NULL,
    observed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_stock_quote_status CHECK (quote_status IN ('LIVE', 'DELAYED', 'MOCK')),
    CONSTRAINT ck_stock_quote_price CHECK (price >= 0),
    CONSTRAINT fk_stock_quotes_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    KEY ix_stock_quotes_latest (company_id, observed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE historical_stock_prices (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    interval_code VARCHAR(12) NOT NULL,
    price_time TIMESTAMP(6) NOT NULL,
    open_price DECIMAL(19, 4) NOT NULL,
    high_price DECIMAL(19, 4) NOT NULL,
    low_price DECIMAL(19, 4) NOT NULL,
    close_price DECIMAL(19, 4) NOT NULL,
    volume BIGINT NULL,
    currency CHAR(3) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_historical_prices_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_historical_price_provider UNIQUE (company_id, interval_code, price_time, provider),
    KEY ix_historical_price_range (company_id, interval_code, price_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE portfolio_holdings (
    id CHAR(36) NOT NULL,
    portfolio_id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    quantity DECIMAL(24, 8) NOT NULL DEFAULT 0,
    average_cost DECIMAL(19, 4) NOT NULL DEFAULT 0,
    cost_basis DECIMAL(19, 4) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_holding_portfolio_company UNIQUE (portfolio_id, company_id),
    CONSTRAINT fk_holdings_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
    CONSTRAINT fk_holdings_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE RESTRICT,
    KEY ix_holdings_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chat_sessions (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    company_id CHAR(36) NULL,
    document_id CHAR(36) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sessions_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE SET NULL,
    KEY ix_chat_sessions_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chat_messages (
    id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    sequence_no BIGINT NOT NULL,
    speaker VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    citations JSON NULL,
    model_name VARCHAR(120) NULL,
    prompt_version VARCHAR(40) NULL,
    token_count INT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_chat_speaker CHECK (speaker IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_message_order UNIQUE (session_id, sequence_no),
    KEY ix_chat_messages_session_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE financial_documents (
    id CHAR(36) NOT NULL,
    owner_id CHAR(36) NOT NULL,
    company_id CHAR(36) NULL,
    financial_year VARCHAR(16) NULL,
    document_type VARCHAR(40) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    checksum CHAR(64) NOT NULL,
    processing_status VARCHAR(20) NOT NULL,
    page_count INT NULL,
    failure_reason VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_document_status CHECK (processing_status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED', 'DELETED')),
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE SET NULL,
    CONSTRAINT uq_document_owner_checksum UNIQUE (owner_id, checksum),
    KEY ix_documents_owner_updated (owner_id, updated_at),
    KEY ix_documents_filters (company_id, financial_year, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE chat_sessions ADD CONSTRAINT fk_chat_sessions_document
    FOREIGN KEY (document_id) REFERENCES financial_documents (id) ON DELETE SET NULL;

CREATE TABLE document_chunks (
    id CHAR(36) NOT NULL,
    document_id CHAR(36) NOT NULL,
    chunk_ordinal INT NOT NULL,
    page_start INT NOT NULL,
    page_end INT NOT NULL,
    content LONGTEXT NOT NULL,
    qdrant_point_id VARCHAR(64) NULL,
    embedding_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_chunk_pages CHECK (page_start > 0 AND page_end >= page_start),
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id) REFERENCES financial_documents (id) ON DELETE CASCADE,
    CONSTRAINT uq_document_chunk_order UNIQUE (document_id, chunk_ordinal),
    KEY ix_document_chunk_pages (document_id, page_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ai_research_reports (
    id CHAR(36) NOT NULL,
    owner_id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    prompt_version VARCHAR(40) NOT NULL,
    model_name VARCHAR(120) NULL,
    result_json JSON NULL,
    failure_reason VARCHAR(500) NULL,
    idempotency_key VARCHAR(100) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reports_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE RESTRICT,
    CONSTRAINT uq_report_idempotency UNIQUE (owner_id, idempotency_key),
    KEY ix_reports_owner_created (owner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE financial_news (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NULL,
    source VARCHAR(100) NOT NULL,
    external_id VARCHAR(200) NOT NULL,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    content MEDIUMTEXT NULL,
    summary MEDIUMTEXT NULL,
    summary_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_news_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE SET NULL,
    CONSTRAINT uq_news_source_external UNIQUE (source, external_id),
    KEY ix_news_published (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE price_alerts (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    comparison VARCHAR(8) NOT NULL,
    threshold_price DECIMAL(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_price_alert_comparison CHECK (comparison IN ('ABOVE', 'BELOW')),
    CONSTRAINT fk_price_alerts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_price_alerts_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    KEY ix_price_alerts_active (company_id, enabled, threshold_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification_events (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    payload JSON NULL,
    dedupe_key VARCHAR(160) NULL,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_notification_dedupe UNIQUE (user_id, dedupe_key),
    KEY ix_notifications_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
