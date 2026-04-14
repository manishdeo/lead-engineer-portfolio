# 🏗️ Architecture Decisions

## ADR-001: Microservices over Monolith

**Context:** E-commerce platforms require independent scaling of catalog browsing (read-heavy) vs order processing (write-heavy).

**Decision:** Decompose into 7 domain-bounded microservices.

**Consequences:**
- ✅ Independent deployment and scaling
- ✅ Team autonomy per service
- ✅ Technology flexibility
- ❌ Increased operational complexity
- ❌ Distributed transaction challenges

---

## ADR-002: Saga Pattern for Distributed Transactions

**Context:** Order creation spans inventory reservation, payment processing, and notification — across 3 services.

**Decision:** Orchestration-based Saga in Order Service (vs choreography).

**Why Orchestration over Choreography:**
- Centralized flow visibility in Order Service
- Easier to debug and monitor
- Explicit compensation logic
- Better for complex multi-step workflows

**Saga Flow:**
```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────────┐
│ Create Order │────▶│ Reserve      │────▶│ Process     │────▶│ Confirm      │
│  (PENDING)   │     │ Inventory    │     │ Payment     │     │ Order        │
└─────────────┘     └──────┬───────┘     └──────┬──────┘     └──────────────┘
                           │ FAIL                │ FAIL
                    ┌──────▼───────┐     ┌──────▼──────┐
                    │ Cancel Order │     │ Release     │
                    │              │◀────│ Inventory + │
                    │              │     │ Refund      │
                    └──────────────┘     └─────────────┘
```

---

## ADR-003: Apache Kafka as Event Bus

**Context:** Services need asynchronous, reliable communication with replay capability.

**Decision:** Kafka over RabbitMQ/SQS.

**Rationale:**
- Log-based: events can be replayed for recovery
- High throughput: handles 100K+ messages/sec
- Consumer groups: multiple consumers per topic
- Ordering guarantees within partitions (partition by orderId)

**Topics:**
| Topic | Producer | Consumers |
|-------|----------|-----------|
| `order-events` | Order Service | Inventory, Payment, Notification |
| `payment-events` | Payment Service | Order, Notification |
| `inventory-events` | Inventory Service | Order, Notification |
| `notification-events` | Notification Service | — |

---

## ADR-004: Database per Service

**Context:** Microservices need data isolation to avoid tight coupling.

**Decision:** Each service owns its PostgreSQL database. Redis shared for caching.

| Service | Database | Cache |
|---------|----------|-------|
| Product | product_db | Redis (catalog cache) |
| Order | order_db | Redis (order status CQRS read model) |
| Payment | payment_db | — |
| Inventory | inventory_db | Redis (stock counts) |
| User | user_db | Redis (session/token cache) |

---

## ADR-005: API Gateway Pattern

**Context:** Need centralized authentication, rate limiting, and routing.

**Decision:** Spring Cloud Gateway as the single entry point.

**Responsibilities:**
- JWT validation
- Rate limiting (Token Bucket: 100 req/min per user)
- Request routing to downstream services
- Request/response logging
- CORS handling

---

## ADR-006: Circuit Breaker with Resilience4j

**Context:** Inter-service calls can fail; need to prevent cascade failures.

**Decision:** Resilience4j Circuit Breaker + Retry + Time Limiter on all synchronous calls.

**Configuration:**
- Sliding window: 10 requests
- Failure threshold: 50%
- Open state duration: 30s
- Retry: 3 attempts with 1s wait
- Timeout: 3s

---

## ADR-007: CQRS for Order Service

**Context:** Order queries (status checks, history) are 10x more frequent than writes.

**Decision:** Write to PostgreSQL, project to Redis for fast reads.

**Write Path:** REST → Order Service → PostgreSQL → Kafka Event
**Read Path:** REST → Order Service → Redis (denormalized view)

---

## ADR-008: JWT Authentication

**Context:** Stateless authentication needed across microservices.

**Decision:** JWT issued by User Service, validated at API Gateway.

**Flow:**
1. User authenticates → User Service issues JWT
2. Client sends JWT in Authorization header
3. API Gateway validates JWT, extracts claims
4. Gateway forwards request with user context headers
5. Downstream services trust gateway-forwarded headers

---

## ADR-009: Idempotency for Payment Processing

**Context:** Network retries can cause duplicate payment charges.

**Decision:** Client-generated idempotency key stored in payment_db.

**Implementation:**
- Client sends `X-Idempotency-Key` header
- Payment Service checks if key exists in DB
- If exists: return cached response
- If not: process payment, store result with key

---

## ADR-010: Observability Stack

**Context:** Distributed systems need comprehensive observability.

**Decision:** Three pillars approach.

| Pillar | Tool |
|--------|------|
| Metrics | Micrometer → Prometheus → Grafana |
| Tracing | Micrometer Tracing → Zipkin |
| Logging | SLF4J + Logback → Structured JSON |

**Trace propagation:** Automatic via Micrometer Tracing (W3C Trace Context).
