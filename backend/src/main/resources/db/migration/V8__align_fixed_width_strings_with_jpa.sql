-- These values are represented as Java Strings, so use VARCHAR consistently.
-- UUID columns remain CHAR(36) and are mapped explicitly as CHAR in the entities.
ALTER TABLE companies
    MODIFY isin VARCHAR(12) NULL,
    MODIFY currency VARCHAR(3) NOT NULL DEFAULT 'INR';

ALTER TABLE portfolios
    MODIFY base_currency VARCHAR(3) NOT NULL DEFAULT 'INR';

ALTER TABLE portfolio_transactions
    MODIFY currency VARCHAR(3) NOT NULL DEFAULT 'INR';

ALTER TABLE refresh_tokens
    MODIFY token_hash VARCHAR(64) NOT NULL,
    MODIFY replaced_by_hash VARCHAR(64) NULL;

ALTER TABLE stock_quotes
    MODIFY currency VARCHAR(3) NOT NULL;

ALTER TABLE historical_stock_prices
    MODIFY currency VARCHAR(3) NOT NULL;

ALTER TABLE financial_documents
    MODIFY checksum VARCHAR(64) NOT NULL;

ALTER TABLE price_alerts
    MODIFY currency VARCHAR(3) NOT NULL DEFAULT 'INR';

ALTER TABLE company_financial_metrics
    MODIFY currency VARCHAR(3) NOT NULL;
