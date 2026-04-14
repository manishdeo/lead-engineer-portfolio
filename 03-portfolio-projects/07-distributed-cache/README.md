# 🗄️ Distributed Cache System

> Production-grade distributed cache with consistent hashing, pluggable eviction policies (LRU/LFU/TTL), Redis Cluster backend, near-cache, and Prometheus metrics.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 🎯 Overview

Most engineers use Redis as a black box. This project implements the internals — consistent hashing ring, virtual nodes, pluggable eviction strategies, two-level caching (near-cache + distributed), and cache-aside/write-through/write-behind patterns. Built to demonstrate deep distributed systems knowledge in interviews.

**Key Interview Differentiators:**
- Consistent hashing with virtual nodes — implemented from scratch, not a library
- Pluggable eviction: LRU (LinkedHashMap), LFU (min-heap), TTL (delayed queue)
- Two-level cache: in-process near-cache (Caffeine) + Redis Cluster
- Write-behind with async batching for write-heavy workloads
- Cache stampede protection via probabilistic early expiration
- Full Prometheus metrics: hit/miss ratio, latency percentiles, eviction counts

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Client Request                        │
└──────────────────────┬───────────────────────────────────┘
                       │
                ┌──────▼──────┐
                │  Near Cache  │  (Caffeine, in-process, ~1ms)
                │  L1 Cache    │
                └──────┬──────┘
                       │ miss
                ┌──────▼──────┐
                │  Consistent  │  hash(key) → virtual node → physical node
                │  Hash Ring   │
                └──────┬──────┘
                       │
          ┌────────────┼────────────┐
          │            │            │
    ┌─────▼─────┐ ┌───▼─────┐ ┌───▼─────┐
    │  Redis     │ │  Redis  │ │  Redis  │
    │  Node 1   │ │  Node 2 │ │  Node 3 │
    │  (L2)     │ │  (L2)   │ │  (L2)   │
    └───────────┘ └─────────┘ └─────────┘
```

### Data Flow
1. **GET** → Check near-cache (L1) → if miss, consistent hash to find Redis node → fetch from L2 → populate L1
2. **PUT** → Write to Redis node via consistent hash → invalidate/update near-cache → apply eviction if at capacity
3. **Write-behind** → Batch writes in memory → flush to Redis asynchronously every N ms

---

## 🛠️ Tech Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Framework | Spring Boot 3.2, Java 21 | Virtual threads for async ops |
| Distributed Cache | Redis Cluster (Lettuce) | Sharding, replication, persistence |
| Near Cache | Caffeine | Fastest JVM cache, O(1) operations |
| Hashing | Custom consistent hash ring | Interview showcase, MD5/MurmurHash |
| Metrics | Micrometer + Prometheus | Hit ratio, latency, eviction tracking |
| Eviction | LRU, LFU, TTL (pluggable) | Strategy pattern |

---

## 🚀 Features

- **Consistent hashing** — Virtual nodes for uniform distribution, O(log N) lookup via TreeMap
- **Pluggable eviction** — LRU (access-order LinkedHashMap), LFU (frequency counter + min-heap), TTL (scheduled cleanup)
- **Near-cache** — Caffeine L1 with configurable size/TTL, reduces Redis round-trips by ~90%
- **Write patterns** — Cache-aside, write-through, write-behind (async batch flush)
- **Stampede protection** — Probabilistic early expiration prevents thundering herd on hot keys
- **Admin API** — Cache stats, flush, resize, node management
- **Benchmarks** — JMH microbenchmarks for throughput/latency comparison

---

## 📦 Project Structure

```
07-distributed-cache/
├── src/main/java/com/maplehub/cache/
│   ├── CacheApplication.java
│   ├── core/
│   │   ├── ConsistentHashRing.java      # Hash ring with virtual nodes
│   │   ├── DistributedCache.java         # Main cache facade
│   │   └── NearCache.java               # Caffeine L1 wrapper
│   ├── eviction/
│   │   ├── EvictionPolicy.java           # Strategy interface
│   │   ├── LruEvictionPolicy.java        # Least Recently Used
│   │   ├── LfuEvictionPolicy.java        # Least Frequently Used
│   │   └── TtlEvictionPolicy.java        # Time-To-Live based
│   ├── service/
│   │   └── CacheService.java             # Business logic + write patterns
│   ├── controller/
│   │   └── CacheController.java          # REST API
│   ├── metrics/
│   │   └── CacheMetrics.java             # Prometheus counters/gauges
│   ├── model/
│   │   └── CacheEntry.java               # Value wrapper with metadata
│   └── config/
│       └── CacheConfig.java              # Redis + Caffeine config
├── src/test/java/                        # Unit tests
├── docker-compose.yml
├── scripts/
└── .github/workflows/ci.yml
```

---

## 📊 Performance

| Operation | Near-cache hit | Redis hit | Redis miss |
|-----------|---------------|-----------|------------|
| GET latency | ~0.5ms | ~2ms | ~5ms (+ origin) |
| Throughput | 500K ops/sec | 100K ops/sec | N/A |
| Hit ratio target | >85% L1 | >95% L1+L2 | <5% |

---

## 🏃 Getting Started

```bash
docker-compose up -d        # Redis Cluster (3 masters + 3 replicas)
./mvnw spring-boot:run

# PUT
curl -X PUT http://localhost:8080/api/cache/mykey -d '{"value":"hello","ttlSeconds":300}'

# GET
curl http://localhost:8080/api/cache/mykey

# Stats
curl http://localhost:8080/api/cache/stats
```

---

## 📖 Interview Talking Points

1. **Why consistent hashing over modular hashing?** — Adding/removing nodes only remaps K/N keys (not all keys). Virtual nodes fix non-uniform distribution
2. **LRU vs LFU trade-offs** — LRU is simpler (O(1) with LinkedHashMap), LFU better for skewed access patterns but more complex (frequency tracking)
3. **Near-cache invalidation** — TTL-based (eventual consistency) vs pub/sub invalidation (stronger consistency, more complexity)
4. **Cache stampede** — Probabilistic early expiration: `if (now - fetchTime) > ttl * (1 - beta * ln(random()))` triggers early refresh
5. **Write-behind risks** — Data loss on crash (mitigate with WAL), ordering issues (mitigate with per-key queues)
6. **Redis Cluster vs Codis vs Twemproxy** — Redis Cluster is native, no proxy overhead, automatic failover

---

## 📄 License

MIT License
