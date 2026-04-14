# 🗄️ Database & Caching Interview Questions (100+)

---

### Q1: SQL vs NoSQL — how do you decide?
**A:**
| Choose SQL when | Choose NoSQL when |
|----------------|-------------------|
| ACID transactions needed | Flexible schema |
| Complex joins/queries | Massive scale (TB+) |
| Data integrity critical | High write throughput |
| Well-defined schema | Key-value access patterns |
| Reporting/analytics | Geo-distributed |

### Q2: Explain database indexing. B-Tree vs Hash index?
**A:**
- **B-Tree:** Sorted, supports range queries, equality. Default in PostgreSQL/MySQL.
- **Hash:** O(1) equality lookups only. No range queries.
- **GIN:** Full-text search, JSONB, arrays (PostgreSQL).
- **BRIN:** Block range index for large sequential data (time-series).

**Rule:** Index columns used in WHERE, JOIN, ORDER BY. But indexes slow writes.

### Q3: What is database connection pooling? Why is it critical?
**A:** Reuse DB connections instead of creating new ones per request. Each connection = ~10MB memory + TCP overhead. Tools: HikariCP (Java), PgBouncer (PostgreSQL). Typical pool: 10-50 connections.

### Q4: Explain PostgreSQL MVCC.
**A:** Multi-Version Concurrency Control — readers don't block writers. Each transaction sees a snapshot. Old versions cleaned up by VACUUM. Enables high concurrency without read locks.

### Q5: What are the Redis data structures and their use cases?
**A:**
| Structure | Use Case |
|-----------|----------|
| String | Cache, counters, rate limiting |
| Hash | Object storage, user sessions |
| List | Message queues, activity feeds |
| Set | Tags, unique visitors |
| Sorted Set | Leaderboards, priority queues |
| Stream | Event log, message streaming |
| HyperLogLog | Cardinality estimation (unique counts) |

### Q6: How do you handle cache stampede?
**A:** When cache expires, many requests hit DB simultaneously.
Solutions:
1. **Locking:** First request acquires lock, others wait
2. **Early expiration:** Refresh cache before TTL expires
3. **Stale-while-revalidate:** Serve stale, refresh in background
4. **Probabilistic early expiration:** Random jitter on TTL

### Q7: What is read replica lag? How do you handle it?
**A:** Replication delay between primary and replica. Solutions:
- Read-your-writes: Route user's reads to primary after their write
- Causal consistency: Track write timestamp, only read from replica if caught up
- Synchronous replication: Guarantees consistency but adds latency

### Q8: Explain database partitioning vs sharding.
**A:**
- **Partitioning:** Split table within same DB instance (range, list, hash)
- **Sharding:** Split data across multiple DB instances
- Partitioning = logical. Sharding = physical distribution.
