# Architecture Decision Records (ADR) – PayRoute

This document details the architectural decisions, design rationales, and trade-offs made during the development of **PayRoute – Intelligent Payment Routing & Failover Engine**.

---

## ADR 001: Selection of Java 21 and Spring Boot 3

### Status
Accepted

### Context
A payment routing and failover engine requires strict type safety, predictable execution characteristics, mature concurrency utilities, and robust enterprise ecosystem support.

### Decision
We selected **Java 21 LTS** with **Spring Boot 3.3.x**.
- **Virtual Threads (Project Loom)** readiness allows high-throughput concurrent HTTP and gateway simulation handling without thread starvation.
- **Spring Boot 3** provides native GraalVM capability, Jakarta EE 10 standards, and optimized Spring Data JPA performance.
- Declarative transaction boundaries (`@Transactional`) guarantee database consistency during complex multi-step payment states.

### Consequences
- Requires JDK 21+ runtime.
- High developer productivity, enterprise-grade dependency injection, and clean separation of concerns.

---

## ADR 002: Dynamic Strategy Pattern for Routing Engine

### Status
Accepted

### Context
Merchants require flexibility in how their payments are routed: some prioritize absolute lowest processing cost (interchange optimization), others demand maximum success rates or sub-100ms round-trip latency, while some want traffic split across multiple gateways by weight.

### Decision
We decoupled routing logic completely from controllers and database code using the **Strategy Pattern**:
- `RoutingStrategy` interface defines a single contract: `rankProviders(candidates, transaction, weights)`.
- Implementations:
  - `PriorityRoutingStrategy`: Deterministic priority ranking.
  - `LowestCostRoutingStrategy`: Computes dynamic transaction fees (`amount * fee_% + flat_fee`) per gateway.
  - `HighestSuccessRateRoutingStrategy`: Dynamically sorts gateways by rolling simulated success metrics.
  - `FastestResponseRoutingStrategy`: Directs traffic to the lowest round-trip latency gateway.
  - `WeightedRoutingStrategy`: Random weighted sampling for traffic distribution.
- `RoutingEngine` filters out inactive providers and applies Circuit Breaker checks before passing healthy candidates to the active strategy.

### Consequences
- Easy to plug in new routing strategies (e.g., ML-based predictive routing, geographic routing) without touching existing execution logic (Open/Closed Principle).

---

## ADR 003: Distributed Idempotency Mechanism

### Status
Accepted

### Context
In distributed payment architectures, network timeouts or retries can cause clients to submit identical payment requests multiple times, leading to duplicate customer charges.

### Decision
We implemented a two-tier idempotency system:
1. **Mandatory Header:** Every payment request must supply an `Idempotency-Key` header.
2. **Database Unique Constraint:** PostgreSQL unique constraint `(merchant_id, idempotency_key)`.
3. **Payload Fingerprinting:** Computes SHA-256 hash of the incoming request body.
4. **Lifecycle States:**
   - **New Key:** Atomic insert with state `IN_PROGRESS`.
   - **Concurrent Duplicate:** If another request with the same key is active, immediately return `409 Conflict`.
   - **Completed Duplicate (Payload Match):** Return the cached JSON response payload immediately with `200 OK` and header `X-Cache-Lookup: HIT`.
   - **Completed Duplicate (Payload Mismatch):** If the key is reused with different amount, currency, or payload, reject with `422 Unprocessable Entity`.

### Consequences
- Absolute guarantee against duplicate processing even under extreme concurrency.
- Thread-safe and persistent across application restarts.

---

## ADR 004: Circuit Breaker and Exponential Failover

### Status
Accepted

### Context
Simulated payment gateways may experience brownouts, elevated failure rates, or network timeouts. Sending traffic to an unhealthy gateway degrades system success rates and delays responses.

### Decision
We built a stateful, thread-safe Circuit Breaker per gateway:
- **States:**
  - `CLOSED`: Normal operation. Tracks consecutive failures. If failures exceed threshold (default: 3), transition to `OPEN`.
  - `OPEN`: Gateway is bypassed. Requests are immediately diverted to alternative providers without incurring gateway latency.
  - `HALF_OPEN`: After a configurable cooldown window (e.g. 15s), a single test transaction is permitted. If successful, state reverts to `CLOSED`; if failed, state returns to `OPEN`.
- **Failover Engine:**
  - Classifies errors into **Retryable** (`GATEWAY_TIMEOUT`, `RATE_LIMIT_EXCEEDED`, `NETWORK_ERROR`) vs **Permanent** (`INSUFFICIENT_FUNDS`, `SUSPECTED_FRAUD`).
  - Automatically switches to candidate #2 with exponential backoff on retryable failures.
  - Permanent failures terminate immediately to prevent unnecessary retries.

### Consequences
- Fast-fails unhealthy providers and achieves resilient 99%+ overall transaction success rates through automatic failover.

---

## ADR 005: Event-Driven Kafka Messaging with Outbox Pattern

### Status
Accepted

### Context
Downstream systems (fraud analysis, ledger updates, customer notifications, merchant webhooks) require notification of payment events asynchronously.

### Decision
- Events: `PAYMENT_INITIATED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `PROVIDER_SWITCHED`, `REFUND_INITIATED`.
- Implemented **Transactional Outbox Pattern**: every event is saved into the PostgreSQL `payment_events` table within the transaction, then dispatched asynchronously to Kafka topic `payroute.payment.events`.
- If Kafka is temporarily unreachable, events remain securely persisted in the database for guaranteed replay and auditability without losing financial transactions.

### Consequences
- Full decoupling between synchronous payment responses and asynchronous audit/notification consumers.

---

## ADR 006: Hybrid Database & Multi-Profile Caching Strategy

### Status
Accepted

### Context
Developers need to run and test the application instantly without mandating a running Docker/PostgreSQL/Kafka daemon, while production requires PostgreSQL 16, Redis 7, and distributed Kafka.

### Decision
- Multi-profile architecture:
  - `docker`: Real PostgreSQL, Redis 7, and Kafka KRaft.
  - `local`: In-memory PostgreSQL-compatible H2 database with Flyway migrations, graceful Redis fallback to `ConcurrentMapCacheManager`, and resilient Kafka dispatch.
- Cache invalidation: Provider configurations and routing rules are cached with TTL and evicted (`@CacheEvict`) upon any administrative configuration change.

### Consequences
- Zero-friction local development and automated CI/CD test execution, combined with production-grade containerized deployment.
