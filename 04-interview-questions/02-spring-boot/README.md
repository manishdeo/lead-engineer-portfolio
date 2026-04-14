# 🍃 Spring Boot & Microservices Interview Questions (200+)

> Comprehensive Q&A for Lead/Principal Software Engineer interviews

---

## 📋 Table of Contents
1. [Spring Boot Core](#spring-boot-core)
2. [Spring Security & JWT](#spring-security--jwt)
3. [Microservices Architecture](#microservices-architecture)
4. [Inter-Service Communication](#inter-service-communication)
5. [Distributed Transactions & Saga](#distributed-transactions--saga)
6. [Resilience Patterns](#resilience-patterns)
7. [Event-Driven Architecture](#event-driven-architecture)
8. [Database & Caching](#database--caching)
9. [Testing](#testing)
10. [Performance & Production](#performance--production)

---

## Spring Boot Core

### Q1: What's new in Spring Boot 3.x vs 2.x?
**A:** Key changes:
- **Jakarta EE 10** (javax → jakarta namespace migration)
- **Java 17+ baseline** (records, sealed classes, pattern matching)
- **Native compilation** via GraalVM (AOT processing)
- **Observability** — Micrometer Observation API replaces Spring Cloud Sleuth
- **Problem Details** (RFC 7807) for error responses
- **HTTP interfaces** — declarative HTTP clients

### Q2: Explain Spring Boot auto-configuration. How does it work internally?
**A:**
1. `@SpringBootApplication` includes `@EnableAutoConfiguration`
2. Spring reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
3. Each auto-config class has `@Conditional` annotations (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, etc.)
4. Spring evaluates conditions and registers beans only when conditions are met
5. User-defined beans always take precedence (`@ConditionalOnMissingBean`)

**Interview tip:** Mention you can debug with `--debug` flag or `spring.autoconfigure.exclude` to disable specific configs.

### Q3: How does Spring Boot handle externalized configuration? What's the property resolution order?
**A:** (Highest to lowest priority)
1. Command-line arguments
2. `SPRING_APPLICATION_JSON`
3. OS environment variables
4. `application-{profile}.yml` (profile-specific)
5. `application.yml`
6. `@PropertySource` annotations
7. Default properties

**Lead-level insight:** In microservices, use Spring Cloud Config Server or AWS Parameter Store for centralized config management.

### Q4: Explain the Spring Bean lifecycle in detail.
**A:**
1. **Instantiation** — Constructor called
2. **Populate properties** — Dependency injection
3. **BeanNameAware** → `setBeanName()`
4. **BeanFactoryAware** → `setBeanFactory()`
5. **ApplicationContextAware** → `setApplicationContext()`
6. **BeanPostProcessor** → `postProcessBeforeInitialization()`
7. **@PostConstruct** / `InitializingBean.afterPropertiesSet()`
8. **BeanPostProcessor** → `postProcessAfterInitialization()`
9. Bean is ready for use
10. **@PreDestroy** / `DisposableBean.destroy()` on shutdown

### Q5: What are the different bean scopes? When would you use prototype scope?
**A:**
- **singleton** (default) — One instance per Spring container
- **prototype** — New instance per injection/request
- **request** — One per HTTP request (web)
- **session** — One per HTTP session (web)
- **application** — One per ServletContext

**Prototype use case:** Stateful beans like command objects, builders, or when you need a fresh instance each time (e.g., non-thread-safe third-party library wrappers).

**Gotcha:** Injecting prototype into singleton gives you only one instance. Use `ObjectProvider<T>` or `@Lookup` to get fresh instances.

---

## Spring Security & JWT

### Q6: How do you implement JWT authentication in a microservices architecture?
**A:**
```
1. User authenticates → Auth Service issues JWT (access + refresh tokens)
2. Client sends JWT in Authorization: Bearer header
3. API Gateway validates JWT signature and expiry
4. Gateway forwards request with user context headers (X-User-Id, X-User-Role)
5. Downstream services trust gateway-forwarded headers (internal network only)
```

**Key decisions:**
- **Token storage:** Access token in memory, refresh token in httpOnly cookie
- **Token rotation:** Short-lived access (15 min), long-lived refresh (7 days)
- **Revocation:** Redis blacklist for logout, or short TTL + refresh rotation

### Q7: How do you handle authorization across microservices?
**A:**
- **API Gateway level:** Role-based route access (ADMIN-only endpoints)
- **Service level:** Method-level security with `@PreAuthorize`
- **Resource level:** Owner-based access (user can only see their orders)

```java
@PreAuthorize("hasRole('ADMIN') or #customerId == authentication.principal.id")
public Order getOrder(String customerId, Long orderId) { ... }
```

### Q8: What's the difference between OAuth2, OIDC, and JWT?
**A:**
- **OAuth2** — Authorization framework (grants access to resources)
- **OIDC** — Identity layer on top of OAuth2 (authentication — who you are)
- **JWT** — Token format (can be used by both OAuth2 and OIDC)

**Analogy:** OAuth2 is the protocol, OIDC adds identity, JWT is the envelope.

---

## Microservices Architecture

### Q9: How do you decompose a monolith into microservices? What's your approach?
**A:** (Lead-level answer using Strangler Fig pattern)
1. **Identify bounded contexts** using Domain-Driven Design (DDD)
2. **Start with the seam** — find natural boundaries in the monolith
3. **Strangler Fig pattern** — Route new traffic to microservice, old traffic to monolith
4. **Database decomposition** — Start with shared DB, then split per service
5. **Event-driven decoupling** — Replace synchronous calls with events
6. **Incremental migration** — One service at a time, not big bang

**Key criteria for splitting:**
- Independent deployment need
- Different scaling requirements
- Different team ownership
- Different technology needs

### Q10: What are the trade-offs of microservices vs monolith?
**A:**

| Aspect | Monolith | Microservices |
|--------|----------|---------------|
| Complexity | Simple to develop | Distributed system complexity |
| Deployment | All-or-nothing | Independent per service |
| Scaling | Vertical | Horizontal per service |
| Data consistency | ACID transactions | Eventual consistency |
| Team autonomy | Shared codebase | Independent teams |
| Debugging | Stack trace | Distributed tracing |
| Latency | In-process calls | Network calls |

**Lead-level insight:** "Start with a well-structured monolith. Extract microservices only when you have a clear scaling or team autonomy need."

### Q11: How do you handle service discovery in microservices?
**A:**
- **Client-side discovery:** Eureka, Consul — client queries registry
- **Server-side discovery:** AWS ALB, Kubernetes Services — load balancer routes
- **DNS-based:** Kubernetes CoreDNS — service-name resolves to ClusterIP

**In Kubernetes:** Service discovery is built-in. `http://order-service:8082` resolves via CoreDNS. No need for Eureka.

### Q12: What's the Database per Service pattern? How do you handle cross-service queries?
**A:**
Each microservice owns its database. Cross-service data access via:
1. **API composition** — Aggregate data from multiple service APIs
2. **CQRS** — Materialized views for read-heavy queries
3. **Event-driven** — Services publish events, consumers build local projections
4. **Saga pattern** — For distributed transactions

**Anti-pattern:** Shared database between services — creates tight coupling.

---

## Inter-Service Communication

### Q13: Synchronous vs Asynchronous communication — when to use which?
**A:**
| Pattern | Use When | Example |
|---------|----------|---------|
| **Sync (REST/gRPC)** | Need immediate response, simple request-reply | Get product details |
| **Async (Kafka/RabbitMQ)** | Fire-and-forget, eventual consistency OK | Order placed → notify |
| **Async with reply** | Long-running operations | Payment processing |

**Rule of thumb:** "If the caller can proceed without waiting, use async."

### Q14: REST vs gRPC vs GraphQL — how do you choose?
**A:**
- **REST** — Standard CRUD, public APIs, browser clients
- **gRPC** — High-performance internal service-to-service, streaming, polyglot
- **GraphQL** — Client-driven queries, mobile apps needing flexible data fetching

**Lead-level:** "We use REST for external APIs, gRPC for internal high-throughput service calls, and GraphQL for our mobile BFF (Backend for Frontend)."

### Q15: How does Kafka differ from RabbitMQ? When would you choose each?
**A:**
| Feature | Kafka | RabbitMQ |
|---------|-------|----------|
| Model | Log-based (pull) | Queue-based (push) |
| Ordering | Per partition | Per queue |
| Replay | Yes (retention) | No (consumed = gone) |
| Throughput | 100K+ msg/sec | 10K msg/sec |
| Use case | Event streaming, event sourcing | Task queues, RPC |

**Choose Kafka when:** Event sourcing, high throughput, replay needed, multiple consumers per event.
**Choose RabbitMQ when:** Simple task queues, routing logic, low latency point-to-point.

---

## Distributed Transactions & Saga

### Q16: Explain the Saga pattern. Orchestration vs Choreography?
**A:**
Saga = sequence of local transactions with compensating actions for rollback.

**Orchestration:**
- Central coordinator (Saga orchestrator) directs the flow
- ✅ Easy to understand, centralized logic
- ❌ Single point of failure, can become complex

**Choreography:**
- Each service listens to events and reacts
- ✅ Loosely coupled, no central coordinator
- ❌ Hard to track flow, cyclic dependencies possible

**When to use which:**
- **Orchestration:** Complex flows (3+ services), need visibility
- **Choreography:** Simple flows (2 services), loose coupling preferred

### Q17: How do you handle distributed transactions without 2PC?
**A:**
1. **Saga pattern** — Compensating transactions
2. **Outbox pattern** — Write event to outbox table in same DB transaction, poll and publish
3. **Event sourcing** — Events are the source of truth
4. **Idempotency** — Handle duplicate messages gracefully

**Why not 2PC?** Blocks resources, doesn't scale, single coordinator failure = all blocked.

### Q18: What is the Outbox Pattern? Why is it important?
**A:**
Problem: Need to update DB AND publish event atomically. Dual-write problem.

Solution:
1. Write business data + event to outbox table in **same DB transaction**
2. Separate process (CDC or poller) reads outbox and publishes to Kafka
3. Mark as published

**Tools:** Debezium (CDC), or custom scheduled poller.

---

## Resilience Patterns

### Q19: Explain Circuit Breaker pattern. How does Resilience4j implement it?
**A:**
States: **CLOSED** → **OPEN** → **HALF_OPEN**

- **CLOSED:** Requests flow normally. Track failure rate.
- **OPEN:** All requests fail fast (no call to downstream). Wait duration.
- **HALF_OPEN:** Allow limited requests. If successful → CLOSED. If fail → OPEN.

```java
@CircuitBreaker(name = "payment-service", fallbackMethod = "paymentFallback")
@Retry(name = "payment-service")
@TimeLimiter(name = "payment-service")
public PaymentResponse processPayment(PaymentRequest req) { ... }
```

### Q20: What's the Bulkhead pattern?
**A:**
Isolate resources so failure in one area doesn't cascade.

**Thread pool bulkhead:** Separate thread pools per downstream service.
**Semaphore bulkhead:** Limit concurrent calls per service.

**Example:** If Payment Service is slow, it only exhausts its own thread pool (10 threads), not the entire Order Service thread pool (200 threads).

### Q21: How do you implement retry with exponential backoff?
**A:**
```yaml
resilience4j:
  retry:
    instances:
      payment-service:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        # Retries: 1s, 2s, 4s
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - com.example.BusinessException
```

**Key:** Only retry on transient failures (network, timeout). Never retry on business errors (invalid input).

---

## Event-Driven Architecture

### Q22: What is Event Sourcing? When would you use it?
**A:**
Instead of storing current state, store all state changes as immutable events.

**Use when:**
- Need complete audit trail (financial, healthcare)
- Need temporal queries ("what was the state at time T?")
- Need event replay for debugging or rebuilding read models
- Domain naturally event-driven (booking, trading)

**Don't use when:**
- Simple CRUD with no audit needs
- Team unfamiliar with eventual consistency

### Q23: What is CQRS? How does it relate to Event Sourcing?
**A:**
**CQRS** = Command Query Responsibility Segregation
- **Command side:** Handles writes (create, update, delete)
- **Query side:** Handles reads (optimized read models)

**With Event Sourcing:**
- Commands produce events → stored in event store
- Events projected into read-optimized views (Redis, Elasticsearch)
- Read model can be rebuilt by replaying events

### Q24: How do you handle event ordering and exactly-once processing?
**A:**
**Ordering:** Kafka guarantees order within a partition. Use entity ID as partition key.
**Exactly-once:**
- Kafka: `enable.idempotence=true` + transactional producer
- Consumer: Idempotent processing (check if event already processed via dedup table)
- Outbox pattern: Ensures at-least-once, consumer handles dedup

---

## Database & Caching

### Q25: Redis vs Memcached — when to use which?
**A:**
| Feature | Redis | Memcached |
|---------|-------|-----------|
| Data structures | Strings, Lists, Sets, Hashes, Sorted Sets | Strings only |
| Persistence | RDB + AOF | None |
| Pub/Sub | Yes | No |
| Clustering | Redis Cluster | Client-side sharding |
| Use case | Caching, sessions, locks, queues | Simple key-value caching |

**Lead answer:** "Redis for anything beyond simple caching — distributed locks, rate limiting, session store, pub/sub."

### Q26: What caching strategies do you use? Cache-aside vs Write-through?
**A:**
- **Cache-aside (Lazy):** App checks cache → miss → read DB → populate cache
- **Write-through:** App writes to cache → cache writes to DB
- **Write-behind:** App writes to cache → cache async writes to DB (risky)
- **Read-through:** Cache itself fetches from DB on miss

**Most common:** Cache-aside with TTL. Simple, works for most read-heavy workloads.

### Q27: How do you handle cache invalidation in microservices?
**A:**
1. **TTL-based:** Set expiry, accept stale data within TTL window
2. **Event-driven:** Service publishes event on data change → consumer evicts cache
3. **Write-through:** Update cache on every write
4. **Versioned keys:** `product:v2:123` — new version = new key

**"There are only two hard things in CS: cache invalidation and naming things."**

---

## Testing

### Q28: How do you test microservices? What's your testing strategy?
**A:** Testing pyramid:
1. **Unit tests** (70%) — Service layer, business logic, mocks for dependencies
2. **Integration tests** (20%) — Repository + DB (Testcontainers), Kafka consumers
3. **Contract tests** (5%) — Pact/Spring Cloud Contract for API compatibility
4. **E2E tests** (5%) — Full flow through multiple services

**Key tools:**
- **Testcontainers** — Real PostgreSQL, Kafka, Redis in Docker for integration tests
- **WireMock** — Mock external service APIs
- **Spring Cloud Contract** — Consumer-driven contract testing

### Q29: How do you use Testcontainers for integration testing?
**A:**
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

---

## Performance & Production

### Q30: How do you handle API rate limiting?
**A:**
- **Token Bucket** — Fixed rate, allows bursts (most common)
- **Sliding Window** — Smooth rate limiting
- **Fixed Window** — Simple but allows burst at window boundary

**Implementation:** Redis + Lua script for distributed rate limiting, or Spring Cloud Gateway's built-in `RequestRateLimiter`.

### Q31: How do you implement distributed tracing?
**A:**
- **Micrometer Tracing** (Spring Boot 3) — Replaces Spring Cloud Sleuth
- **W3C Trace Context** — Standard trace propagation headers
- **Zipkin/Jaeger** — Trace collection and visualization

Each request gets a `traceId` propagated across all services. Spans track individual operations.

### Q32: What metrics do you monitor for microservices in production?
**A:** RED method:
- **Rate** — Requests per second
- **Errors** — Error rate (4xx, 5xx)
- **Duration** — Latency percentiles (p50, p95, p99)

Plus:
- JVM metrics (heap, GC, threads)
- Kafka consumer lag
- Circuit breaker state
- Database connection pool utilization
- Cache hit/miss ratio

---

## Quick-Fire Questions (Q33-Q50)

### Q33: What is Spring WebFlux? When would you use it over Spring MVC?
**A:** Reactive, non-blocking web framework. Use for high-concurrency I/O-bound workloads (API gateways, streaming). Stick with MVC for CPU-bound or simple CRUD.

### Q34: What's the difference between @Component, @Service, @Repository, @Controller?
**A:** All are `@Component` stereotypes. `@Repository` adds exception translation. `@Controller` adds request mapping. `@Service` is semantic only. Use the right one for clarity.

### Q35: How does Spring handle circular dependencies?
**A:** Constructor injection fails. Field/setter injection works via proxy (not recommended). Best fix: redesign to break the cycle using events or a mediator.

### Q36: What is Spring AOP? Common use cases?
**A:** Aspect-Oriented Programming for cross-cutting concerns: logging, security, transactions, caching, metrics. Uses proxies (JDK dynamic or CGLIB).

### Q37: Explain @Transactional propagation levels.
**A:**
- **REQUIRED** (default) — Join existing or create new
- **REQUIRES_NEW** — Always create new (suspend existing)
- **NESTED** — Savepoint within existing
- **SUPPORTS** — Use existing if available, else non-transactional
- **MANDATORY** — Must have existing, else exception

### Q38: What is Spring Cloud Config? Alternatives?
**A:** Centralized configuration server. Alternatives: AWS Parameter Store, HashiCorp Vault, Kubernetes ConfigMaps, Consul KV.

### Q39: How do you handle API versioning?
**A:** URI path (`/api/v2/orders`), header (`Accept: application/vnd.api.v2+json`), or query param. URI path is most common and explicit.

### Q40: What is the API Gateway pattern? Benefits?
**A:** Single entry point for all clients. Benefits: authentication, rate limiting, routing, load balancing, request transformation, CORS, logging.

### Q41: How do you handle idempotency in REST APIs?
**A:** Client sends `Idempotency-Key` header. Server checks if key exists → return cached response. If not → process and store result with key.

### Q42: What is the Strangler Fig pattern?
**A:** Gradually replace monolith by routing new features to microservices while keeping old features in monolith. Eventually, monolith is fully replaced.

### Q43: How do you handle distributed logging?
**A:** Structured JSON logs + correlation ID (traceId) + centralized aggregation (ELK Stack, CloudWatch Logs, Datadog).

### Q44: What is a Service Mesh? When do you need one?
**A:** Infrastructure layer for service-to-service communication (Istio, Linkerd). Provides: mTLS, traffic management, observability, retries. Need it when you have 50+ services.

### Q45: How do you handle database migrations in microservices?
**A:** Flyway or Liquibase per service. Backward-compatible migrations only (expand-contract pattern). Never break existing consumers.

### Q46: What is the Sidecar pattern?
**A:** Deploy helper container alongside main container in same pod. Use cases: logging agent, service mesh proxy (Envoy), config sync.

### Q47: How do you handle graceful shutdown in Spring Boot?
**A:** `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s`. Stops accepting new requests, completes in-flight requests, then shuts down.

### Q48: What is Spring Native / GraalVM? Benefits?
**A:** Compile Spring Boot to native binary. Benefits: ~10ms startup (vs 2-5s), lower memory. Trade-offs: longer build time, reflection limitations, no runtime class loading.

### Q49: How do you implement health checks for microservices?
**A:** Spring Actuator `/actuator/health` with custom health indicators for DB, Kafka, Redis. Kubernetes uses readiness/liveness probes pointing to these endpoints.

### Q50: What's your approach to API documentation?
**A:** OpenAPI 3.0 (Springdoc) auto-generated from code + annotations. Swagger UI for interactive testing. Versioned docs per API version.

---

*Continue to [System Design Q&A](../04-system-design/README.md) →*
