CREATE TABLE stock_explorer_listings (
    id CHAR(36) NOT NULL,
    exchange_code VARCHAR(8) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    series VARCHAR(8) NOT NULL,
    listing_date DATE NULL,
    paid_up_value DECIMAL(15,4) NULL,
    market_lot INT NULL,
    isin VARCHAR(12) NULL,
    face_value DECIMAL(15,4) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_stock_explorer_exchange_symbol UNIQUE (exchange_code, symbol),
    KEY ix_stock_explorer_name (company_name),
    KEY ix_stock_explorer_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_explorer_import_state (
    source_key VARCHAR(64) NOT NULL,
    last_import_date DATE NOT NULL,
    last_imported_at TIMESTAMP(6) NOT NULL,
    records_imported INT NOT NULL,
    PRIMARY KEY (source_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
