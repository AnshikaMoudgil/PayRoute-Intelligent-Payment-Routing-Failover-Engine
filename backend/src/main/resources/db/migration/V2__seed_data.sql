-- PayRoute Seed Data Migration V2
-- Initial seed for Payment Providers, Merchants, and Routing Rules

-- 1. Insert Payment Providers
INSERT INTO payment_providers (
    id, code, name, status, base_fee_percentage, base_fee_flat, avg_latency_ms, simulated_success_rate, failure_probability, priority, created_at, updated_at
) VALUES 
('a0000000-0000-0000-0000-000000000001', 'PROVIDER_A', 'ApexPay Gateway', 'ACTIVE', 1.80, 2.00, 120, 98.00, 2.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b0000000-0000-0000-0000-000000000002', 'PROVIDER_B', 'BoltGateway Direct', 'ACTIVE', 1.20, 1.00, 190, 93.00, 7.00, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0000000-0000-0000-0000-000000000003', 'PROVIDER_C', 'CloudSettle Fast', 'ACTIVE', 2.10, 1.50, 75, 96.00, 4.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Insert Seed Merchants
INSERT INTO merchants (
    id, merchant_code, name, email, api_key_hash, raw_api_key_prefix, status, default_routing_strategy, created_at, updated_at
) VALUES 
('10000000-0000-0000-0000-000000000001', 'MERCH-ACME-001', 'Acme Retail Corp', 'payments@acmeretail.com', 'fad927d1807baea45853a4996f421a0e86b29f270f735bcbb4a35cb4282de362', 'pr_live_acme_***', 'ACTIVE', 'LOWEST_COST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('10000000-0000-0000-0000-000000000002', 'MERCH-NOVA-002', 'Nova Digital Services', 'billing@novadigital.io', 'ab1bbacbc4bb7c6e0791c2ac9579da3d54674bae4f48060cd34dbc7928fe5693', 'pr_live_nova_***', 'ACTIVE', 'HIGHEST_SUCCESS_RATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Insert Default Routing Rules
-- Global Priority Rule
INSERT INTO routing_rules (
    id, merchant_id, strategy, provider_weights, min_amount, max_amount, is_active, created_at, updated_at
) VALUES 
('20000000-0000-0000-0000-000000000001', NULL, 'PRIORITY', '{"PROVIDER_A": 50, "PROVIDER_B": 30, "PROVIDER_C": 20}', 0.00, 10000000.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Acme Custom Lowest Cost Rule
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'LOWEST_COST', '{"PROVIDER_B": 60, "PROVIDER_A": 25, "PROVIDER_C": 15}', 0.00, 5000000.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Nova Custom Highest Success Rate Rule
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'HIGHEST_SUCCESS_RATE', '{"PROVIDER_A": 70, "PROVIDER_C": 20, "PROVIDER_B": 10}', 0.00, 5000000.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
