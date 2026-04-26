-- Seed stocks (mix of global + KASE-listed companies)
INSERT INTO stocks (ticker, name, sector, base_price, current_price, total_shares, currency, exchange, listed_at) VALUES
('AAPL',  'Apple Inc.',              'Technology',             182.50, 182.50, 15728700000, 'USD', 'NASDAQ',    CURRENT_TIMESTAMP),
('MSFT',  'Microsoft Corporation',   'Technology',             378.85, 378.85,  7432000000, 'USD', 'NASDAQ',    CURRENT_TIMESTAMP),
('GOOGL', 'Alphabet Inc.',           'Technology',             141.80, 141.80,  6250000000, 'USD', 'NASDAQ',    CURRENT_TIMESTAMP),
('AMZN',  'Amazon.com Inc.',         'Consumer Discretionary', 178.25, 178.25, 10300000000, 'USD', 'NASDAQ',    CURRENT_TIMESTAMP),
('JPM',   'JPMorgan Chase & Co.',    'Financials',             196.50, 196.50,  2900000000, 'USD', 'NYSE',      CURRENT_TIMESTAMP),
('TSLA',  'Tesla Inc.',              'Consumer Discretionary', 245.00, 245.00,  3185000000, 'USD', 'NASDAQ',    CURRENT_TIMESTAMP),
('KASPI', 'Kaspi.kz JSC',           'Financials',              85.40,  85.40,  1900000000, 'USD', 'KASE/NASDAQ', CURRENT_TIMESTAMP),
('HSBK',  'Halyk Bank JSC',         'Financials',               8.75,   8.75,  2400000000, 'USD', 'KASE',      CURRENT_TIMESTAMP);

-- Seed demo portfolios
INSERT INTO portfolios (user_id, display_name, cash_balance, created_at) VALUES
('user1', 'Demo Trader Alpha', 100000.00, CURRENT_TIMESTAMP),
('user2', 'Demo Trader Beta',   50000.00, CURRENT_TIMESTAMP);

-- Seed initial positions
INSERT INTO positions (portfolio_id, ticker, quantity, average_cost) VALUES
(1, 'AAPL',  50, 180.00),
(1, 'MSFT',  20, 370.00),
(1, 'KASPI', 100, 82.00),
(2, 'TSLA',  30, 240.00),
(2, 'HSBK',  500,  8.50);
