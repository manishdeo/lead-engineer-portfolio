# 🏗️ System Design Interview Questions (100+ Scenarios)

---

## Top 10 Must-Know Systems

| # | System | Key Patterns |
|---|--------|-------------|
| 1 | URL Shortener | Hashing, base62, read-heavy cache |
| 2 | Twitter/Social Feed | Fan-out, timeline, pub/sub |
| 3 | Uber/Ride-Sharing | Geospatial, matching, real-time |
| 4 | Netflix/Streaming | CDN, transcoding, adaptive bitrate |
| 5 | WhatsApp/Chat | WebSocket, message queue, presence |
| 6 | Payment System | Idempotency, saga, reconciliation |
| 7 | Notification System | Push, email, SMS, priority queues |
| 8 | Rate Limiter | Token bucket, sliding window, Redis |
| 9 | Search Engine | Inverted index, ranking, crawling |
| 10 | E-commerce | Inventory, cart, checkout, recommendations |

---

## Quick-Fire Design Questions

### Q1: How would you design a URL shortener?
**Key points:** Base62 encoding, counter-based or hash-based ID generation, 301 redirect, Redis cache for hot URLs, analytics tracking, TTL for expiry.

### Q2: How would you design a rate limiter?
**Key points:** Token bucket (allows bursts) vs sliding window (smooth). Redis + Lua script for distributed. Headers: `X-RateLimit-Remaining`, `Retry-After`.

### Q3: How would you design a notification system?
**Key points:** Priority queue (urgent vs batch), multi-channel (push, email, SMS), template engine, user preferences, rate limiting per user, delivery tracking.

### Q4: How would you design a chat system like WhatsApp?
**Key points:** WebSocket for real-time, message queue for offline delivery, read receipts, end-to-end encryption, group chat fan-out, presence service.

### Q5: How would you design a distributed cache?
**Key points:** Consistent hashing for partitioning, LRU eviction, write-through vs write-behind, cache stampede prevention (locking), TTL.

### Q6: How would you design a job scheduler?
**Key points:** Priority queue, cron expression parsing, distributed locking (only one instance executes), retry with backoff, dead-letter queue, idempotent jobs.

### Q7: How would you design a file storage system like Google Drive?
**Key points:** Chunked upload, deduplication (content hash), metadata DB, block storage (S3), sync protocol (delta sync), sharing/permissions, versioning.

### Q8: How would you design a metrics/monitoring system?
**Key points:** Time-series DB (InfluxDB, Prometheus), push vs pull model, aggregation (1min, 5min, 1hr), alerting rules, dashboard visualization, retention policies.

### Q9: How would you design an API gateway?
**Key points:** Routing, authentication, rate limiting, request transformation, circuit breaker, logging, caching, load balancing. Tools: Kong, Spring Cloud Gateway.

### Q10: How would you design a recommendation engine?
**Key points:** Collaborative filtering, content-based, hybrid. Offline batch (Spark) + online real-time (feature store). A/B testing for quality. Cold start problem.

---

## Design Principles to Mention

- **Single Responsibility** — Each service does one thing well
- **Loose Coupling** — Services communicate via APIs/events, not shared DB
- **High Cohesion** — Related functionality grouped together
- **Idempotency** — Safe to retry any operation
- **Graceful Degradation** — System works (reduced) when components fail
- **Defense in Depth** — Multiple layers of security
- **Design for Failure** — Assume everything will fail

---

*See detailed designs in [System Design Mastery](../../02-system-design/README.md)*
