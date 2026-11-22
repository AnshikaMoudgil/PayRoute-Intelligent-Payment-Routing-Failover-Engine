-- PayRoute Database Schema Migration V1
-- Normalized schema for payment orchestration, idempotency, routing, attempts, refunds, and event tracking

CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    merchant_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    api_key_hash VARCHAR(128) NOT NULL UNIQUE,
    raw_api_key_prefix VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    default_routing_strategy VARCHAR(50) NOT NULL DEFAULT 'PRIORITY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_providers (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    base_fee_percentage NUMERIC(5, 2) NOT NULL DEFAULT 1.50,
    base_fee_flat NUMERIC(10, 2) NOT NULL DEFAULT 2.00,
    avg_latency_ms BIGINT NOT NULL DEFAULT 150,
    simulated_success_rate NUMERIC(5, 2) NOT NULL DEFAULT 95.00,
    failure_probability NUMERIC(5, 2) NOT NULL DEFAULT 5.00,
    priority INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE routing_rules (
    id UUID PRIMARY KEY,
    merchant_id UUID REFERENCES merchants(id) ON DELETE CASCADE,
    strategy VARCHAR(50) NOT NULL,
    provider_weights TEXT,
    min_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    max_amount NUMERIC(15, 2) NOT NULL DEFAULT 100000000.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(128) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    selected_provider_id UUID REFERENCES payment_providers(id),
    routing_strategy_used VARCHAR(50),
    total_attempts INT NOT NULL DEFAULT 0,
    fee_charged NUMERIC(15, 2) DEFAULT 0.00,
    customer_id VARCHAR(100),
    customer_email VARCHAR(150),
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchant_idempotency UNIQUE (merchant_id, idempotency_key)
);

CREATE TABLE provider_attempts (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES payment_transactions(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES payment_providers(id),
    attempt_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    latency_ms BIGINT NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    is_failover BOOLEAN NOT NULL DEFAULT FALSE,
    raw_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_payload TEXT,
    response_code INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idem_merchant_key UNIQUE (merchant_id, idempotency_key)
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES payment_transactions(id) ON DELETE RESTRICT,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    amount NUMERIC(15, 2) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    provider_refund_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    published_to_kafka BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX idx_trans_merchant_created ON payment_transactions(merchant_id, created_at DESC);
CREATE INDEX idx_trans_status ON payment_transactions(status);
CREATE INDEX idx_trans_created_at ON payment_transactions(created_at);
CREATE INDEX idx_attempts_trans ON provider_attempts(transaction_id);
CREATE INDEX idx_refunds_trans ON refunds(transaction_id);
CREATE INDEX idx_events_trans ON payment_events(transaction_id);
CREATE INDEX idx_idem_merchant_key ON idempotency_records(merchant_id, idempotency_key);
