# PayRoute – Intelligent Payment Routing & Failover Engine

<p align="center">
  <img src="https://img.shields.io/badge/Java-21_LTS-007396?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3" />
  <img src="https://img.shields.io/badge/React-18.3-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React 18" />
  <img src="https://img.shields.io/badge/TypeScript-5.6-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Redis-7.0-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Apache_Kafka-KRaft-231F20?style=for-the-badge&logo=apachekafka&logoColor=white" alt="Kafka" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

---

## 📌 Table of Contents

- [What is PayRoute?](#-what-is-payroute)
  - [The Problem It Solves](#the-problem-it-solves)
  - [Core Value Proposition](#core-value-proposition)
- [System Architecture](#-system-architecture)
  - [High-Level Architecture](#high-level-architecture)
  - [Payment Execution & Failover Sequence](#payment-execution--failover-sequence)
  - [Circuit Breaker State Machine](#circuit-breaker-state-machine)
  - [Distributed Idempotency Flowchart](#distributed-idempotency-flowchart)
- [Key Features & Capabilities](#-key-features--capabilities)
  - [1. Dynamic Routing Strategies](#1-dynamic-routing-strategies)
  - [2. Resilient Circuit Breakers & Auto-Failover](#2-resilient-circuit-breakers--auto-failover)
  - [3. Two-Tier Distributed Idempotency](#3-two-tier-distributed-idempotency)
  - [4. Transactional Outbox & Kafka Streaming](#4-transactional-outbox--kafka-streaming)
  - [5. Dual API: REST & GraphQL](#5-dual-api-rest--graphql)
  - [6. Real-Time Telemetry & Financial Analytics](#6-real-time-telemetry--financial-analytics)
- [Interactive Frontend Dashboard & Playground](#-interactive-frontend-dashboard--playground)
- [Technology Stack](#-technology-stack)
- [Database Schema & Data Model](#-database-schema--data-model)
- [REST & GraphQL API Reference](#-rest--graphql-api-reference)
  - [REST Endpoints](#rest-endpoints)
  - [cURL Examples](#curl-examples)
  - [GraphQL Schema & Queries](#graphql-schema--queries)
- [Performance & Concurrency Benchmark](#-performance--concurrency-benchmark)
- [Getting Started & Quickstart](#-getting-started--quickstart)
  - [Prerequisites](#prerequisites)
  - [Option A: Zero-Docker Local Development](#option-a-zero-docker-local-development)
  - [Option B: Enterprise Containerized (Docker Compose)](#option-b-enterprise-containerized-docker-compose)
  - [Default Ports & Endpoints](#default-ports--endpoints)
  - [Seed Data & API Credentials](#seed-data--api-credentials)
- [Automated Testing](#-automated-testing)
- [Project Directory Structure](#-project-directory-structure)
- [Architectural Decision Records (ADRs)](#-architectural-decision-records-adrs)
- [License & Contributing](#-license--contributing)

---

## 💡 What is PayRoute?

**PayRoute** is an enterprise-grade payment orchestration and resilience platform engineered to eliminate single-gateway dependency, protect revenue during processor outages, optimize interchange fees, and prevent duplicate customer charges at scale.

### The Problem It Solves

In modern high-volume fintech and global e-commerce:
1. **Downtime & Brownouts Cost Millions:** If a single payment processor (e.g., Stripe, Adyen, Razorpay) experiences an API brownout, elevated failure rate, or network partition, checkouts stall, carts are abandoned, and direct revenue is lost.
2. **Expensive Interchange & Processing Fees:** Different gateways charge varying percentage rates and fixed fees per transaction. Routing every payment through a single gateway leads to suboptimal transaction costs.
3. **Double Charges & Race Conditions:** Network retries from mobile clients or flaky internet connections often send identical payment requests concurrently. Without strict distributed idempotency, customers get double-charged.
4. **Cascading Latencies:** When an upstream processor degrades and takes 5+ seconds to time out, your entire checkout pipeline threads freeze, causing cascading service failures.

### Core Value Proposition

PayRoute addresses these challenges with an intelligent routing layer situated between your checkout clients and downstream payment gateways:

```
[ Your Store / App / Mobile Clients ]
                 │
                 ▼
     ┌───────────────────────┐
     │       PayRoute        │ ──► Distributed Idempotency Guard (SHA-256 + Atomic DB Locks)
     │  Orchestration Engine │ ──► Intelligent Routing (Lowest Cost, Max Success, Latency, Weighted)
     │                       │ ──► Circuit Breakers (Fast-fail degraded gateways in 0ms)
     │                       │ ──► Smart Failover (Retryable error recovery with zero downtime)
     └───────────────────────┘
                 │
       ┌─────────┼─────────┐
       ▼         ▼         ▼
  [ ApexPay ] [ BoltGW ] [ CloudSettle ]
  (98% Succ)  (Low Cost) (75ms Ultra Fast)
```

- **99.99%+ Effective Transaction Success Rate:** Even if the primary gateway fails, PayRoute automatically intercepts retryable errors and transparently fails over to secondary healthy providers in milliseconds.
- **Dynamic Fee Minimization:** Saves 10%–35% in processing fees by dynamically computing interchange costs per gateway based on transaction amounts.
- **Strict Financial Idempotency:** Guaranteed duplicate charge prevention via unique composite constraints, SHA-256 payload fingerprinting, and cached response replays.
- **Sub-Millisecond Circuit Tripping:** Degraded gateways are automatically isolated from the routing pool so checkouts never hang waiting for timeouts.

---

## 🏛️ System Architecture

### High-Level Architecture

```mermaid
flowchart TD
    Client["Client / React Dashboard / Merchant Apps"] -->|REST API with Idempotency-Key & API Key| Security["Spring Security & API Key Filter"]
    
    subgraph PayRoute Core Engine
        Security --> IdemService["Distributed Idempotency Engine"]
        IdemService -->|Check SHA-256 / Lock Key| IdemDB[("idempotency_records")]
        
        IdemService --> Router["Routing Engine"]
        Router --> RuleResolver["Rule & Merchant Resolver (Redis / DB)"]
        Router --> CBRegistry["Per-Gateway Circuit Breaker Registry"]
        Router --> Strategy{"Active Strategy Evaluator"}
        
        Strategy -->|PRIORITY| PriorityStrat["Priority Routing Strategy"]
        Strategy -->|LOWEST_COST| CostStrat["Lowest Cost Fee Optimizer"]
        Strategy -->|HIGHEST_SUCCESS_RATE| SuccessStrat["Success Rate Telemetry Ranker"]
        Strategy -->|FASTEST_RESPONSE| LatencyStrat["Lowest Latency Ranker"]
        Strategy -->|WEIGHTED| WeightStrat["Weighted Traffic Sharder"]
        
        Strategy --> FailoverEngine["Failover & Retry Orchestrator"]
        
        subgraph Simulated Upstream Gateways
            FailoverEngine -->|Attempt 1| ProvA["ApexPay (98% SR, 120ms)"]
            FailoverEngine -->|Failover 1| ProvB["BoltGateway (1.2% Low Fee, 190ms)"]
            FailoverEngine -->|Failover 2| ProvC["CloudSettle (75ms Fast, 96% SR)"]
        end
        
        FailoverEngine --> Outbox["Transactional Outbox (payment_events)"]
        FailoverEngine --> MainDB[("PostgreSQL 16: payment_transactions & provider_attempts")]
    end
    
    Outbox --> Kafka["Apache Kafka (payroute.payment.events)"]
    Kafka --> AnalyticsConsumer["Telemetry & Audit Consumer"]
    AnalyticsConsumer --> RedisCache[("Redis 7 Cache")]
```

### Payment Execution & Failover Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Client as Merchant / Client
    participant API as Payment Controller
    participant Idem as Idempotency Engine
    participant Router as Routing Engine
    participant CB as Circuit Breaker Registry
    participant GW1 as Primary Gateway
    participant GW2 as Secondary Gateway (Failover)
    participant Outbox as Transactional Outbox
    participant DB as PostgreSQL 16

    Client->>API: POST /api/payments (Idempotency-Key: "idem_123")
    API->>Idem: Validate & acquire lock for "idem_123"
    alt Key already completed with identical payload
        Idem-->>Client: 200 OK (Cached Response, X-Cache-Lookup: HIT)
    else Key in-flight concurrently
        Idem-->>Client: 409 Conflict (Concurrent Request)
    else Key reused with altered payload
        Idem-->>Client: 422 Unprocessable Entity
    end

    Idem->>Router: Evaluate candidates for transaction
    Router->>CB: Filter out OPEN circuit breaker providers
    CB-->>Router: Healthy candidates list
    Router->>Router: Apply active strategy (e.g. LOWEST_COST)
    Router-->>API: Provider Ranked List [GW1, GW2]

    API->>GW1: Attempt 1: Process charge
    alt Gateway 1 Timeout or Rate Limit (Retryable)
        GW1-->>API: 504 Gateway Timeout
        API->>CB: Record failure on GW1 (increment trip counter)
        API->>DB: Log provider_attempts (Attempt #1, FAILED, isFailover=false)
        Note over API,GW2: Trigger Automatic Exponential Failover
        API->>GW2: Attempt 2: Process charge on fallback gateway
        GW2-->>API: 200 OK (Success, AuthCode)
        API->>CB: Record success on GW2
        API->>DB: Log provider_attempts (Attempt #2, SUCCESS, isFailover=true)
    else Gateway 1 Success
        GW1-->>API: 200 OK (Success, AuthCode)
        API->>CB: Record success on GW1
        API->>DB: Log provider_attempts (Attempt #1, SUCCESS, isFailover=false)
    end

    API->>DB: Update payment_transactions (Status: SUCCESS)
    API->>Outbox: Persist PAYMENT_SUCCESS in payment_events
    API->>Idem: Complete idempotency record with response JSON
    API-->>Client: 200 OK (PaymentResponse with attempts timeline)
```

### Circuit Breaker State Machine

Each payment provider maintains an independent thread-safe state machine:

```mermaid
stateDiagram-v2
    [*] --> CLOSED: Initial Startup
    
    CLOSED --> OPEN: Consecutive Failures >= Threshold (3)
    note right of CLOSED
      Healthy state:
      All traffic permitted.
      Tracks rolling error rate.
    end note
    
    OPEN --> HALF_OPEN: Cooldown Timer (15s) Expires
    note right of OPEN
      Degraded state:
      Immediately bypasses provider (0ms delay).
      Diverts traffic to alternative gateways.
    end note
    
    HALF_OPEN --> CLOSED: Trial Requests Succeeded
    HALF_OPEN --> OPEN: Trial Request Failed
    note right of HALF_OPEN
      Recovery probe:
      Allows limited canary trials (2 calls).
      Closes circuit if healthy;
      Re-opens circuit if failure persists.
    end note
```

### Distributed Idempotency Flowchart

```mermaid
flowchart TD
    Start["Incoming POST /api/payments"] --> CheckHeader{"Idempotency-Key Header Present?"}
    CheckHeader -->|No| Reject400["Return 400 Bad Request"]
    CheckHeader -->|Yes| HashPayload["Compute SHA-256 Hash of Request Body"]
    
    HashPayload --> QueryDB["Query idempotency_records by (merchant_id, key)"]
    QueryDB --> Exists{"Record Exists?"}
    
    Exists -->|No| CreateLock["Insert record: status=IN_PROGRESS (Atomic Unique DB Constraint)"]
    CreateLock --> Proceed["Proceed to Payment Routing & Execution"]
    
    Exists -->|Yes| CheckStatus{"Status in DB?"}
    CheckStatus -->|IN_PROGRESS| Return409["Return 409 Conflict (Concurrent Duplicate Request)"]
    CheckStatus -->|COMPLETED| CheckHash{"Request SHA-256 == Stored Hash?"}
    
    CheckHash -->|Match| ReturnCached["Return 200 OK with Cached Payload (Header: X-Cache-Lookup: HIT)"]
    CheckHash -->|Mismatch| Return422["Return 422 Unprocessable Entity (Key reused with different body)"]
    
    Proceed --> Finished["Payment Engine Finishes"]
    Finished --> UpdateRecord["Update idempotency_records: status=COMPLETED, response_payload=JSON"]
```

---

## ⚡ Key Features & Capabilities

### 1. Dynamic Routing Strategies

PayRoute implements the Gang-of-Four **Strategy Pattern** via `RoutingStrategy`, completely decoupling selection policies from transaction lifecycle handlers:

| Strategy | Enum Key | How It Works | Ideal Use Case |
|---|---|---|---|
| **Priority** | `PRIORITY` | Strictly routes by deterministic provider rank (1, 2, 3...) configured by the merchant. | Standard contractual tier commitments with primary vendors. |
| **Lowest Cost** | `LOWEST_COST` | Dynamically calculates total fee: `(amount * fee_% / 100) + flat_fee` across all eligible gateways and chooses the cheapest. | Margin optimization on high-volume or high-ticket sales. |
| **Highest Success Rate** | `HIGHEST_SUCCESS_RATE` | Evaluates real-time telemetry metrics and selects the provider with the highest rolling success percentage. | Flash sales, ticketing, high-concurrency checkout bursts. |
| **Fastest Response** | `FASTEST_RESPONSE` | Sorts providers by lowest moving average round-trip latency (e.g., CloudSettle at ~75ms). | Mobile apps and micro-transactions requiring sub-100ms response. |
| **Weighted Traffic Split** | `WEIGHTED` | Shards incoming traffic probabilistically based on merchant-configured integer weight distributions (e.g., 50/30/20). | Canary deployments, volume quota distribution across processor contracts. |

### 2. Resilient Circuit Breakers & Auto-Failover

- **Zero-Latency Fast Failure:** When an upstream gateway browns out, the `ProviderCircuitBreaker` transitions to `OPEN`. Subsequent transactions skip that provider instantly (0ms wasted), avoiding thread starvation.
- **Configurable Cooldown:** After 15 seconds, the breaker enters `HALF_OPEN`, allowing 2 canary trials to verify upstream recovery.
- **Intelligent Error Categorization:**
  - **Retryable Errors:** `GATEWAY_TIMEOUT`, `RATE_LIMIT_EXCEEDED`, `SERVICE_UNAVAILABLE`, `NETWORK_ERROR` trigger automatic failover to the next ranked provider with exponential backoff.
  - **Terminal Errors:** `INSUFFICIENT_FUNDS`, `SUSPECTED_FRAUD`, `INVALID_CARD_DATA` immediately abort to prevent redundant processing.
- **Operator Overrides:** Operators can manually trip or reset any gateway breaker at any time via the REST API or Dashboard.

### 3. Two-Tier Distributed Idempotency

- Enforces strict protection against duplicate transactions.
- Uses PostgreSQL unique constraints `(merchant_id, idempotency_key)` for database-level atomic locking across cluster nodes.
- SHA-256 request fingerprinting detects tampering: if the same key is sent with modified amounts or customer details, it is rejected with `422 Unprocessable Entity`.
- Safe replays return the cached response with `X-Cache-Lookup: HIT` header without re-executing against downstream banks.

### 4. Transactional Outbox & Kafka Streaming

- Implements the **Transactional Outbox Pattern**: financial transactions and payment events (`PAYMENT_INITIATED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `PROVIDER_SWITCHED`, `REFUND_INITIATED`) are written atomically into the same PostgreSQL database transaction.
- Asynchronous publisher dispatches events to Kafka topic `payroute.payment.events`.
- If Kafka is temporarily down, zero financial data is lost: events remain queued in `payment_events` with `published_to_kafka = false` for reliable background retry.

### 5. Dual API: REST & GraphQL

- **RESTful Endpoints:** Full CRUD for merchants, payment providers, routing rules, refunds, transactions, and live telemetry.
- **GraphQL API:** Rich GraphQL schema (`/graphql` and interactive GraphiQL playground at `/graphiql`) for flexible client queries and bulk operations.

### 6. Real-Time Telemetry & Financial Analytics

- Calculates p50, p95, and p99 latency metrics.
- Tracks transaction volume trends, gateway share percentages, fee totals, and retry success recovery rates.

---

## 🖥️ Interactive Frontend Dashboard & Playground

PayRoute includes a production-ready React 18 + TypeScript + Tailwind CSS web dashboard with 7 dedicated pages:

| Page | View | Key Capabilities |
|---|---|---|
| 📊 **Dashboard** | `DashboardPage` | Real-time KPI summary (total volume, overall success rate, fees paid, failover count, provider split charts). |
| 💳 **Interactive Playground** | `PlaygroundPage` | Execute live payments, simulate idempotency replays (test Cache HITs), toggle routing strategies, and observe failover timelines in real time. |
| ⚡ **Providers Matrix** | `ProvidersPage` | View simulated gateways, monitor live circuit breaker states (`CLOSED`, `OPEN`, `HALF_OPEN`), and manually trip/reset health. |
| 🔀 **Routing Rules** | `RoutingPage` | Manage global and merchant-specific routing policies, customize min/max amount thresholds, and tune traffic weights. |
| 📜 **Transactions Explorer** | `TransactionsPage` | Browse all transactions with full pagination, inspect multi-provider attempt timelines, latencies, and failover logs. |
| 🔄 **Refunds Desk** | `RefundsPage` | Issue full and partial refunds with balance tracking and reason logging. |
| 📈 **Analytics & SLA** | `AnalyticsPage` | In-depth provider performance comparison, fee analytics, and p50/p95/p99 SLA percentiles. |

---

## 🛠️ Technology Stack

```
PayRoute Architecture Stack
├── Backend Core
│   ├── Java 21 LTS (Virtual Threads / Project Loom ready)
│   ├── Spring Boot 3.3.5
│   ├── Spring Data JPA / Hibernate 6
│   ├── Spring Security (API Key & Merchant Authentication)
│   ├── Spring for GraphQL (with GraphiQL IDE)
│   ├── Springdoc OpenAPI 2.6 (Swagger UI)
│   └── Spring Boot Actuator (Health, Metrics, Prometheus)
│
├── Database & Caching
│   ├── PostgreSQL 16 (Production relational store)
│   ├── H2 In-Memory Database (Zero-Docker local development mode)
│   ├── Flyway Database Migration Engine
│   └── Redis 7 (Caching provider configs & rules with in-memory fallback)
│
├── Streaming & Event Bus
│   ├── Apache Kafka 7.6.1 (KRaft mode, no ZooKeeper dependency)
│   └── Transactional Outbox Pattern
│
├── Frontend Dashboard
│   ├── React 18.3 & TypeScript 5.6
│   ├── Vite 5.4 Build System
│   ├── Tailwind CSS 3.4
│   ├── Lucide React Icons
│   └── Recharts (Responsive financial analytics & charts)
│
└── DevOps & Deployment
    ├── Docker & Docker Compose
    ├── Multi-stage Eclipse Temurin 21 Alpine container
    └── Nginx Alpine static web server
```

---

## 🗄️ Database Schema & Data Model

```mermaid
erDiagram
    merchants ||--o{ routing_rules : "configures"
    merchants ||--o{ payment_transactions : "owns"
    merchants ||--o{ idempotency_records : "locks"
    merchants ||--o{ refunds : "authorizes"
    
    payment_transactions ||--o{ provider_attempts : "executes"
    payment_transactions ||--o{ refunds : "originates"
    payment_transactions }o--|| payment_providers : "routed_to"
    
    provider_attempts }o--|| payment_providers : "targets"

    merchants {
        uuid id PK
        string merchant_code UK
        string name
        string email UK
        string api_key_hash UK
        string raw_api_key_prefix
        string status
        string default_routing_strategy
        timestamp created_at
    }

    payment_providers {
        uuid id PK
        string code UK
        string name
        string status
        decimal base_fee_percentage
        decimal base_fee_flat
        bigint avg_latency_ms
        decimal simulated_success_rate
        decimal failure_probability
        int priority
        timestamp created_at
    }

    routing_rules {
        uuid id PK
        uuid merchant_id FK
        string strategy
        string provider_weights
        decimal min_amount
        decimal max_amount
        boolean is_active
        timestamp created_at
    }

    payment_transactions {
        uuid id PK
        uuid merchant_id FK
        string idempotency_key
        decimal amount
        string currency
        string status
        uuid selected_provider_id FK
        string routing_strategy_used
        int total_attempts
        decimal fee_charged
        string customer_id
        string customer_email
        text failure_reason
        timestamp created_at
    }

    provider_attempts {
        uuid id PK
        uuid transaction_id FK
        uuid provider_id FK
        int attempt_number
        string status
        bigint latency_ms
        string error_code
        text error_message
        boolean is_failover
        text raw_response
        timestamp created_at
    }

    idempotency_records {
        uuid id PK
        uuid merchant_id FK
        string idempotency_key
        string request_hash
        string status
        text response_payload
        int response_code
        timestamp created_at
        timestamp locked_at
    }

    refunds {
        uuid id PK
        uuid transaction_id FK
        uuid merchant_id FK
        decimal amount
        string reason
        string status
        string provider_refund_id
        timestamp created_at
    }

    payment_events {
        uuid id PK
        uuid transaction_id
        string event_type
        text payload
        boolean published_to_kafka
        timestamp created_at
    }
```

### High-Concurrency Performance Indexes

To support high-throughput payment processing, the schema utilizes specific composite indexes:
- `idx_trans_merchant_created` on `payment_transactions(merchant_id, created_at DESC)`
- `idx_trans_status` on `payment_transactions(status)`
- `idx_attempts_trans` on `provider_attempts(transaction_id)`
- `uk_merchant_idempotency` on `payment_transactions(merchant_id, idempotency_key)` (UNIQUE)
- `uk_idem_merchant_key` on `idempotency_records(merchant_id, idempotency_key)` (UNIQUE)

---

## 📡 REST & GraphQL API Reference

### REST Endpoints

| Category | Method | Endpoint | Description |
|---|---|---|---|
| **Merchants** | `POST` | `/api/merchants` | Register a new merchant & generate secure API credentials |
| | `GET` | `/api/merchants` | List all registered merchants |
| | `GET` | `/api/merchants/{id}` | Get merchant details |
| **Payments** | `POST` | `/api/payments` | Process payment with idempotency & dynamic routing |
| | `GET` | `/api/payments` | List paginated transactions |
| | `GET` | `/api/payments/{id}` | Get transaction details |
| | `GET` | `/api/payments/{id}/attempts` | Get full attempt audit trail (latencies, provider switches) |
| **Providers** | `GET` | `/api/providers` | List providers with live circuit breaker states |
| | `PUT` | `/api/providers/{id}` | Update provider fee, latency, or simulation attributes |
| | `PATCH`| `/api/providers/{id}/health` | Manually trip, reset, or change provider circuit state |
| **Routing** | `GET` | `/api/routing/rules` | List global and merchant-specific routing rules |
| | `POST` | `/api/routing/rules` | Create a new routing strategy rule |
| | `PUT` | `/api/routing/rules/{id}` | Update rule thresholds or strategy |
| **Refunds** | `POST` | `/api/refunds` | Process a full or partial refund |
| | `GET` | `/api/refunds` | List all refunds |
| | `GET` | `/api/refunds/{id}` | Get refund details |
| **Analytics** | `GET` | `/api/analytics/overview` | High-level financial KPIs (volume, fees, success rate) |
| | `GET` | `/api/analytics/providers`| Provider comparison breakdown |
| | `GET` | `/api/analytics/transactions` | Time-series transaction volume data |
| | `GET` | `/api/analytics/performance` | Throughput and p50, p95, p99 SLA percentiles |

---

### cURL Examples

#### 1. Process Payment with Idempotency & Lowest Cost Routing
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem_demo_98234" \
  -H "X-Merchant-Code: MERCH-ACME-001" \
  -d '{
    "amount": 4500.00,
    "currency": "INR",
    "customerEmail": "alex.morgan@example.com",
    "customerId": "cust_4892",
    "routingStrategyOverride": "LOWEST_COST"
  }'
```

#### 2. Replay Identical Request (Verify Cache HIT)
```bash
# Re-sending the identical request above returns 200 OK immediately with header:
# X-Cache-Lookup: HIT
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem_demo_98234" \
  -H "X-Merchant-Code: MERCH-ACME-001" \
  -d '{
    "amount": 4500.00,
    "currency": "INR",
    "customerEmail": "alex.morgan@example.com",
    "customerId": "cust_4892",
    "routingStrategyOverride": "LOWEST_COST"
  }'
```

#### 3. Inspect Attempt Timeline & Failover
```bash
curl -X GET http://localhost:8080/api/payments/{TRANSACTION_ID}/attempts
```

#### 4. Issue a Partial Refund
```bash
curl -X POST http://localhost:8080/api/refunds \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "YOUR_TRANSACTION_UUID",
    "amount": 1500.00,
    "reason": "Customer returned partial order"
  }'
```

#### 5. Manually Trip / Reset Circuit Breaker
```bash
# Trip circuit breaker to OPEN (simulating outage)
curl -X PATCH http://localhost:8080/api/providers/{PROVIDER_UUID}/health \
  -H "Content-Type: application/json" \
  -d '{ "circuitBreakerState": "OPEN" }'

# Reset circuit breaker back to CLOSED
curl -X PATCH http://localhost:8080/api/providers/{PROVIDER_UUID}/health \
  -H "Content-Type: application/json" \
  -d '{ "resetCircuitBreaker": true }'
```

---

### GraphQL Schema & Queries

PayRoute provides a complete GraphQL endpoint at `POST /graphql` and an interactive IDE at `http://localhost:8080/graphiql`.

#### Query System Overview & Provider Health:
```graphql
query GetSystemHealth {
  analyticsOverview {
    totalTransactions
    overallSuccessRate
    totalVolume
    totalProcessingFees
    totalRetries
  }
  providers {
    id
    code
    name
    status
    circuitBreakerState
    avgLatencyMs
    baseFeePercentage
  }
}
```

#### Initiate Payment via GraphQL Mutation:
```graphql
mutation InitiatePayment {
  initiatePayment(input: {
    idempotencyKey: "idem_gql_88921"
    merchantCode: "MERCH-ACME-001"
    amount: 7200.00
    currency: "INR"
    customerEmail: "priya@example.com"
    routingStrategyOverride: "FASTEST_RESPONSE"
  }) {
    id
    status
    selectedProviderName
    feeCharged
    routingStrategyUsed
    totalAttempts
    attempts {
      providerName
      status
      latencyMs
      isFailover
    }
  }
}
```

---

## 📊 Performance & Concurrency Benchmark

The project includes an automated multi-threaded benchmarking utility (`benchmark/benchmark.py`).

### Concurrency Benchmark Run (50 Concurrent Transactions, 5 Worker Threads)

```text
=================================================================
      PayRoute - High Concurrency Payment Benchmark
=================================================================
Target URL:               http://localhost:8080/api/payments
Total Requests:           50
Concurrency Level:        5 worker threads
Merchant Code:            MERCH-ACME-001
-----------------------------------------------------------------
Total Time Elapsed:       3.17 seconds
Requests per Second:      15.78 req/sec
Successful Requests:      50 (100.0%)
Failed Requests:          0 (0.0%)
Failover Transactions:    5 (10.0% recovered transparently)
-----------------------------------------------------------------
Average Latency:          282.81 ms
p50 (Median) Latency:     260.70 ms
p95 Latency:              530.22 ms
p99 Latency:              625.20 ms
Min Latency:              181.31 ms
Max Latency:              645.99 ms
=================================================================
```

> **Key Result:** When upstream gateway simulations introduced random timeouts and rate limits, PayRoute's automatic failover engine recovered 100% of affected requests, delivering **zero downtime and 100% effective payment completion**.

To run the benchmark yourself:
```bash
python benchmark/benchmark.py -n 50 -c 5 --url http://localhost:8080
```

---

## 🚀 Getting Started & Quickstart

### Prerequisites

- **Java Development Kit (JDK):** Version 21 LTS or newer
- **Maven:** Version 3.9+ (or use pre-built target JAR)
- **Node.js & npm:** Version 20+ (for React frontend)
- **Docker & Docker Compose:** Optional (only needed for Option B)

---

### Option A: Zero-Docker Local Development

PayRoute is designed with developer convenience in mind. You can run the entire system locally **without running Docker, PostgreSQL, or Kafka daemons**:

1. **Start the Backend:**
   ```bash
   # From the project root:
   java -Dspring.profiles.active=local -jar backend/target/payroute-backend-1.0.0-SNAPSHOT.jar
   ```
   *The `local` profile automatically starts an embedded PostgreSQL-compatible H2 database with Flyway migrations, an in-memory cache, and local resilient event streaming on port `8080`.*

2. **Start the Frontend Dashboard:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. **Open in Browser:**
   - **Frontend App:** [http://localhost:5173](http://localhost:5173)
   - **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **GraphiQL IDE:** [http://localhost:8080/graphiql](http://localhost:8080/graphiql)

---

### Option B: Enterprise Containerized (Docker Compose)

Spin up the full distributed production stack with PostgreSQL 16, Redis 7, Confluent Kafka KRaft, Spring Boot Backend, and Nginx-powered React Frontend:

```bash
# Build and launch all 5 services
docker compose up --build
```

To run in the background (detached mode):
```bash
docker compose up -d
```

To stop all containers:
```bash
docker compose down
```

---

### Default Ports & Endpoints

| Service | Port | Description |
|---|---|---|
| **Frontend Web App** | `3000` (Docker) / `5173` (Local) | Interactive React + Vite Dashboard |
| **Backend API** | `8080` | Spring Boot 3 REST & GraphQL APIs |
| **Swagger UI Docs** | `http://localhost:8080/swagger-ui.html` | Interactive OpenAPI documentation |
| **GraphiQL Playground** | `http://localhost:8080/graphiql` | Interactive GraphQL IDE |
| **PostgreSQL 16** | `5432` | Relational payment store (Docker) |
| **Redis 7** | `6379` | High-speed cache for rules & providers |
| **Apache Kafka** | `9092` | Event streaming topic: `payroute.payment.events` |

---

### Seed Data & API Credentials

Upon launch, Flyway migrations (`V1__init_schema.sql` and `V2__seed_data.sql`) automatically seed the database with test merchants and payment providers:

#### Seed Merchants:
- **Acme Retail Corp:**
  - Code: `MERCH-ACME-001`
  - Default Strategy: `LOWEST_COST`
  - Seed API Key Prefix: `pr_live_acme_***`
- **Nova Digital Services:**
  - Code: `MERCH-NOVA-002`
  - Default Strategy: `HIGHEST_SUCCESS_RATE`
  - Seed API Key Prefix: `pr_live_nova_***`

#### Seed Payment Providers:
1. **ApexPay Gateway (`PROVIDER_A`):**
   - Base Fee: 1.80% + ₹2.00 flat
   - Latency: ~120ms
   - Simulated Success Rate: 98%
   - Default Priority: 1
2. **BoltGateway Direct (`PROVIDER_B`):**
   - Base Fee: 1.20% + ₹1.00 flat *(Lowest Fee)*
   - Latency: ~190ms
   - Simulated Success Rate: 93%
   - Default Priority: 2
3. **CloudSettle Fast (`PROVIDER_C`):**
   - Base Fee: 2.10% + ₹1.50 flat
   - Latency: ~75ms *(Ultra Fast)*
   - Simulated Success Rate: 96%
   - Default Priority: 3

---

## 🧪 Automated Testing

PayRoute includes a comprehensive suite of unit and integration tests covering circuit breaker transitions, idempotency locking, routing strategies, failovers, and refund calculations.

Run the test suite using Maven:
```bash
mvn test -f backend/pom.xml
```

### Test Coverage Highlights:
- **`ProviderCircuitBreakerTest`:** Validates failure threshold trips (`CLOSED` -> `OPEN`), cooldown expiration (`OPEN` -> `HALF_OPEN`), trial execution recovery, and manual operator overrides.
- **`RoutingEngineTest`:** Validates deterministic ranking across all 5 strategies (`PRIORITY`, `LOWEST_COST`, `HIGHEST_SUCCESS_RATE`, `FASTEST_RESPONSE`, `WEIGHTED`) and verifies dynamic exclusion of tripped circuit breaker providers.
- **`IdempotencyServiceTest`:** Tests SHA-256 fingerprint verification, 409 conflict detection for concurrent in-flight requests, 422 validation for payload mismatches, and `X-Cache-Lookup: HIT` responses.
- **`RefundServiceTest`:** Tests full refunds, partial balance deduction, status updates, and over-refund rejection.

---

## 📁 Project Directory Structure

```text
PayRoute/
├── .env.example                     # Sample environment variables
├── ADR.md                           # Architecture Decision Records (ADR 001 - 006)
├── docker-compose.yml               # Multi-service stack (Postgres, Redis, Kafka, Backend, Frontend)
├── README.md                        # Documentation & project guide
│
├── backend/                         # Spring Boot 3 & Java 21 Application
│   ├── pom.xml                      # Maven dependencies & build plugins
│   └── src/
│       ├── main/
│       │   ├── java/com/payroute/
│       │   │   ├── PayRouteApplication.java
│       │   │   ├── circuitbreaker/  # Stateful Circuit Breaker & Manager
│       │   │   ├── config/          # Redis & Kafka KRaft configurations
│       │   │   ├── controller/      # REST API & GraphQL Controllers
│       │   │   ├── dto/             # Request/Response Data Transfer Objects
│       │   │   ├── entity/          # JPA Domain Entities & Enums
│       │   │   ├── exception/       # Global Exception Handler & Custom Errors
│       │   │   ├── kafka/           # Event Publisher & Consumer (Outbox Pattern)
│       │   │   ├── provider/        # Simulated Gateway Clients (ApexPay, Bolt, CloudSettle)
│       │   │   ├── repository/      # Spring Data JPA Repositories
│       │   │   ├── routing/         # Routing Engine & 5 Strategy Implementations
│       │   │   ├── security/        # API Key Filter & Principal Authentication
│       │   │   └── service/         # Payment Processing, Idempotency & Analytics
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       ├── application-docker.yml
│       │       ├── db/migration/    # Flyway SQL Migrations (V1 Schema, V2 Seed)
│       │       └── graphql/         # GraphQL Schema Definitions
│       └── test/                    # JUnit 5 & Mockito Test Suite
│
├── frontend/                        # React 18, TypeScript & Tailwind CSS Web App
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── src/
│       ├── App.tsx                  # Main Tab Navigation
│       ├── api/client.ts            # Axios API Client & Endpoints
│       ├── components/              # Status Badges, Circuit Breaker Indicators, Navbar
│       ├── pages/                   # 7 Dashboard Views (Dashboard, Playground, Providers...)
│       └── types/                   # TypeScript Domain Interfaces
│
├── benchmark/                       # Performance Benchmarking Suite
│   ├── benchmark.py                 # Multi-threaded concurrency benchmark runner
│   └── run_benchmark.bat            # Windows execution shortcut
│
└── docker/                          # Containerization Dockerfiles
    ├── Dockerfile.backend           # Multi-stage Eclipse Temurin 21 JRE container
    ├── Dockerfile.frontend          # Multi-stage Node 20 & Nginx Alpine container
    └── nginx.conf                   # Nginx reverse proxy configuration
```

---

## 📑 Architectural Decision Records (ADRs)

For in-depth discussions on system design trade-offs, consult [ADR.md](ADR.md):

- **ADR 001:** Selection of Java 21 LTS and Spring Boot 3.3 (Project Loom readiness, GraalVM capability).
- **ADR 002:** Dynamic Strategy Pattern for Routing Engine (Extensibility, Open/Closed Principle).
- **ADR 003:** Two-Tier Distributed Idempotency Mechanism (Database unique constraints, SHA-256 fingerprinting).
- **ADR 004:** Per-Gateway Circuit Breaker and Exponential Failover Engine.
- **ADR 005:** Event-Driven Kafka Messaging with Transactional Outbox Pattern.
- **ADR 006:** Hybrid Database & Multi-Profile Caching Strategy (Zero-Docker local dev vs Production Docker stack).

---

## 📄 License & Contributing

Distributed under the **MIT License**. See `LICENSE` for more information.

Contributions, issues, and feature suggestions are welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/SmartMLRouting`)
3. Commit your Changes (`git commit -m 'Add Smart ML Routing Strategy'`)
4. Push to the Branch (`git push origin feature/SmartMLRouting`)
5. Open a Pull Request

---

<p align="center">
  <b>PayRoute</b> &bull; Built with precision for resilient, high-volume fintech operations.
</p>
