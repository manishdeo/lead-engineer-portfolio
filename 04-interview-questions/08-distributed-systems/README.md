# 🌐 Distributed Systems Interview Questions (80+)

---

## Core Concepts

### Q1: Explain CAP theorem. Is it really a choice of 2 out of 3?
**A:** In a distributed system, during a network partition (P), you must choose between Consistency (C) and Availability (A). P is not optional — networks fail. So the real choice is **CP vs AP**.
- **CP:** Reject requests during partition to maintain consistency (MongoDB, HBase)
- **AP:** Serve requests during partition, accept stale data (Cassandra, DynamoDB)

### Q2: What is eventual consistency? Give a real-world example.
**A:** After a write, reads may return stale data temporarily, but eventually all replicas converge. Example: DNS propagation — update a DNS record, takes minutes/hours to propagate globally, but eventually all DNS servers have the new value.

### Q3: Explain consensus algorithms. Raft vs Paxos?
**A:** Consensus = getting distributed nodes to agree on a value.
- **Paxos:** Original, mathematically proven, hard to implement
- **Raft:** Designed for understandability, leader-based, used in etcd/Consul
- **Raft phases:** Leader election → Log replication → Safety

### Q4: What is a distributed lock? How do you implement one?
**A:**
- **Redis:** `SETNX key value EX ttl` — simple but not safe during failover
- **Redlock:** Redis author's algorithm using N independent Redis instances
- **ZooKeeper:** Ephemeral sequential nodes — robust but complex
- **Database:** `SELECT FOR UPDATE` — simple but doesn't scale

**Gotcha:** Redis locks can fail during master failover. Use Redlock or ZooKeeper for critical sections.

### Q5: What is consistent hashing? Why is it important?
**A:** Distributes data across nodes such that adding/removing a node only remaps ~1/N keys (vs rehashing everything). Used in: load balancers, distributed caches, Cassandra, DynamoDB.

### Q6: Explain the split-brain problem.
**A:** Network partition causes two groups of nodes to each think they're the leader. Both accept writes → data divergence. Solutions: quorum-based decisions (majority wins), fencing tokens, STONITH.

### Q7: What is vector clock? How does it differ from Lamport clock?
**A:**
- **Lamport clock:** Single counter, establishes causal ordering but can't detect concurrent events
- **Vector clock:** Array of counters (one per node), can detect both causality AND concurrency. Used in DynamoDB for conflict detection.

### Q8: What is the two-phase commit (2PC)? Why is it problematic?
**A:** Coordinator asks all participants to prepare → all vote yes → coordinator commits. Problems: blocking (coordinator fails = all blocked), single point of failure, doesn't scale. Prefer Saga pattern for microservices.

### Q9: What is gossip protocol?
**A:** Nodes periodically exchange state with random peers. Eventually all nodes converge. Used for: failure detection (Cassandra), membership (Consul), state dissemination. Pros: scalable, fault-tolerant. Cons: eventual, not immediate.

### Q10: How does database sharding work? What are the strategies?
**A:**
- **Range-based:** Shard by ID range (1-1M, 1M-2M). Pro: range queries. Con: hotspots.
- **Hash-based:** Hash(key) % N. Pro: even distribution. Con: no range queries, resharding pain.
- **Directory-based:** Lookup table maps key → shard. Pro: flexible. Con: lookup overhead.
- **Consistent hashing:** Best for dynamic scaling.
