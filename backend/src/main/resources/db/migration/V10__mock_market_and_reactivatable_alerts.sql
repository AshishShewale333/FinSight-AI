ALTER TABLE price_alerts
    ADD COLUMN trigger_count INT NOT NULL DEFAULT 0 AFTER last_triggered_at;

-- Seed the configured local practice symbols into the transaction-eligible company catalog.
-- Prices remain synthetic and are labeled LOCAL_DEMO_MOCK by the mock provider.
INSERT IGNORE INTO companies (id, exchange_code, symbol, company_name, isin, currency, active) VALUES
    ('10000000-0000-4000-8000-000000000001', 'NSE', 'RELIANCE', 'Reliance Industries Limited', NULL, 'INR', TRUE),
    ('10000000-0000-4000-8000-000000000002', 'NSE', 'TCS', 'Tata Consultancy Services Limited', NULL, 'INR', TRUE),
    ('10000000-0000-4000-8000-000000000003', 'NSE', 'INFY', 'Infosys Limited', NULL, 'INR', TRUE),
    ('10000000-0000-4000-8000-000000000004', 'NSE', 'HDFCBANK', 'HDFC Bank Limited', NULL, 'INR', TRUE),
    ('10000000-0000-4000-8000-000000000005', 'NSE', 'ICICIBANK', 'ICICI Bank Limited', NULL, 'INR', TRUE),
    ('10000000-0000-4000-8000-000000000006', 'NSE', 'SBIN', 'State Bank of India', NULL, 'INR', TRUE),
    ('10000000-0000-4000-8000-000000000007', 'NSE', 'WIPRO', 'Wipro Limited', NULL, 'INR', TRUE),
    ('10000000-0000-4000-8000-000000000008', 'NSE', 'TATAMOTORS', 'Tata Motors Limited', NULL, 'INR', TRUE);
