# 🌐 Distributed Systems — Interview Reference

---

## Core Theorems & Models

### CAP Theorem
- **Consistency**: All nodes see same data simultaneously
- **Availability**: System remains operational
- **Partition Tolerance**: System continues despite network failures
- **Reality**: P is mandatory in distributed systems → choose CP or AP

### PACELC Extension (2025 interviews ask this)
If **P**artition → choose **A** or **C**. **E**lse → choose **L**atency or **C**onsistency.
- DynamoDB: PA/EL (available during partition, low latency otherwise)
- PostgreSQL: PC/EC (consistent always)
- Cassandra: PA/EL (tunable per query)

### Consistency Models
| Model | Guarantee | Example |
|-------|-----------|--------|
| **Linearizability** | Real-time ordering | Spanner, ZooKeeper |
| **Sequential** | Global order, not real-time | Distributed locks |
| **Causal** | Cause-before-effect ordering | DynamoDB (session) |
| **Eventual** | Converges eventually | Cassandra, DNS |

---

## Consensus & Coordination

| Algorithm | Model | Used By |
|-----------|-------|--------|
| **Raft** | Leader-based, understandable | etcd, Consul, CockroachDB |
| **Paxos** | Quorum-based, proven | Chubby, Spanner |
| **PBFT** | Byzantine fault tolerant | Blockchain |
| **Gossip** | Epidemic, eventually consistent | Cassandra, Consul (membership) |

---

## Fault Tolerance Patterns

| Pattern | Purpose | Implementation |
|---------|---------|---------------|
| **Circuit Breaker** | Prevent cascade failures | Resilience4j, Istio |
| **Bulkhead** | Isolate failure domains | Thread pool / semaphore isolation |
| **Retry + Backoff** | Handle transient failures | Exponential backoff + jitter |
| **Timeout** | Prevent indefinite blocking | Connect + read timeouts |
| **Fallback** | Graceful degradation | Cache, default response |
| **Rate Limiting** | Protect from overload | Token bucket, sliding window |

---

## Data Partitioning & Replication

### Sharding Strategies
- **Hash-based**: Even distribution, no range queries
- **Range-based**: Range queries, risk of hotspots
- **Consistent Hashing**: Minimal remapping on node add/remove (used by DynamoDB, Cassandra)

### Replication
- **Single-leader**: One writer, N readers (PostgreSQL, MySQL)
- **Multi-leader**: Multiple writers, conflict resolution (CockroachDB, DynamoDB Global Tables)
- **Leaderless**: Quorum reads/writes (Cassandra, Riak)

---

## Messaging & Streaming

| Technology | Model | Best For |
|-----------|-------|----------|
| **Kafka** | Log-based, pull | Event streaming, event sourcing, high throughput |
| **RabbitMQ** | Queue-based, push | Task queues, RPC, routing |
| **SQS** | Managed queue | Simple decoupling, serverless |
| **Kinesis** | Shard-based stream | Real-time analytics, AWS-native |
| **Pulsar** | Multi-tenant, tiered storage | Multi-tenant streaming (2025 rising) |

---

## 🔥 2025 Trends in Distributed Systems

### OpenTelemetry (OTel)
CNCF standard replacing vendor-specific observability. Unified API for metrics, traces, logs.
```
App → OTel SDK → OTel Collector → Prometheus / Jaeger / Datadog
```
- Auto-instrumentation for Java, Node.js, Python
- W3C Trace Context propagation
- Vendor-neutral — switch backends without code changes

### eBPF (Extended Berkeley Packet Filter)
Kernel-level programmability without sidecars.
- **Cilium**: eBPF-based networking + security for K8s (replacing iptables)
- **Pixie**: eBPF-based observability (no instrumentation needed)
- **Falco**: eBPF-based runtime security
- **Interview angle**: "Sidecarless service mesh" — eBPF replaces Envoy sidecars

### CRDTs (Conflict-free Replicated Data Types)
Data structures that merge automatically without coordination.
- Used in: Figma (real-time collaboration), Redis (CRDT-based replication)
- Types: G-Counter, PN-Counter, LWW-Register, OR-Set
- **Interview angle**: Alternative to OT for collaborative editing

### Deterministic Simulation Testing
Test distributed systems by controlling all non-determinism.
- Used by: FoundationDB, TigerBeetle, Antithesis
- **Interview angle**: "How do you test distributed systems?" → mention simulation testing

---

## Interview Focus Areas

1. **CAP/PACELC** — Trade-off analysis for any system design
2. **Consensus** — Raft leader election, quorum writes
3. **Consistent Hashing** — Implementation and virtual nodes
4. **Event-Driven** — Kafka ordering, exactly-once, idempotency
5. **Distributed Transactions** — Saga vs 2PC, Outbox pattern
6. **Observability** — OpenTelemetry, distributed tracing
7. **Failure Modes** — Split-brain, cascade failure, thundering herd