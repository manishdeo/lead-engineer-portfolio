# 📖 Cheat Sheets & Quick Reference

> Last-minute revision guides for interview day

---

## Available Cheat Sheets

1. [System Design Checklist](#system-design-checklist)
2. [Java Concurrency](#java-concurrency)
3. [Spring Boot Annotations](#spring-boot-annotations)
4. [Kubernetes Commands](#kubernetes-commands)
5. [AWS Services Quick Reference](#aws-services)
6. [SQL vs NoSQL Decision Matrix](#sql-vs-nosql)
7. [CAP Theorem Quick Guide](#cap-theorem)

---

## System Design Checklist

### Step 1: Requirements (5 min)
- [ ] Clarify functional requirements (3-5 core features)
- [ ] Clarify non-functional requirements (latency, throughput, availability)
- [ ] Identify users and scale (DAU, peak QPS)
- [ ] Ask about constraints (budget, timeline, team size)

### Step 2: Capacity Estimation (3 min)
- [ ] Storage: `users × data_per_user × retention`
- [ ] Bandwidth: `QPS × avg_response_size`
- [ ] QPS: `DAU × actions_per_user / 86400`
- [ ] Read:Write ratio

### Step 3: High-Level Design (10 min)
- [ ] Draw core components (client, LB, services, DB, cache)
- [ ] Identify APIs (REST endpoints)
- [ ] Choose communication pattern (sync/async)
- [ ] Database selection (SQL vs NoSQL)

### Step 4: Deep Dive (15 min)
- [ ] Database schema design
- [ ] Caching strategy
- [ ] Data partitioning / sharding
- [ ] Replication strategy
- [ ] Message queue / event-driven patterns

### Step 5: Scale & Reliability (5 min)
- [ ] Horizontal scaling
- [ ] Load balancing
- [ ] CDN (if applicable)
- [ ] Circuit breaker / retry
- [ ] Monitoring & alerting

### Step 6: Trade-offs (2 min)
- [ ] Consistency vs Availability
- [ ] Cost vs Performance
- [ ] Complexity vs Simplicity

---

## Java Concurrency

| Concept | Use When |
|---------|----------|
| `synchronized` | Simple mutual exclusion |
| `ReentrantLock` | Need tryLock, fairness, multiple conditions |
| `ReadWriteLock` | Read-heavy workloads |
| `StampedLock` | Optimistic reads (Java 8+) |
| `AtomicInteger/Long` | Simple counters |
| `ConcurrentHashMap` | Thread-safe map |
| `CompletableFuture` | Async composition |
| `Virtual Threads` | High-concurrency I/O (Java 21) |
| `ExecutorService` | Thread pool management |
| `CountDownLatch` | Wait for N threads to complete |
| `CyclicBarrier` | Synchronize N threads at a point |
| `Semaphore` | Limit concurrent access |

### Thread Pool Sizing
- **CPU-bound:** `threads = cores + 1`
- **I/O-bound:** `threads = cores × (1 + wait_time/compute_time)`

---

## Spring Boot Annotations

| Annotation | Purpose |
|-----------|---------|
| `@SpringBootApplication` | Main class (= @Configuration + @EnableAutoConfiguration + @ComponentScan) |
| `@RestController` | REST controller (= @Controller + @ResponseBody) |
| `@Service` | Business logic layer |
| `@Repository` | Data access layer (+ exception translation) |
| `@Transactional` | Transaction management |
| `@Cacheable` | Cache method result |
| `@CacheEvict` | Remove from cache |
| `@Async` | Run method asynchronously |
| `@Scheduled` | Scheduled task |
| `@CircuitBreaker` | Resilience4j circuit breaker |
| `@Retry` | Resilience4j retry |
| `@PreAuthorize` | Method-level security |
| `@Valid` | Bean validation |
| `@ConditionalOnProperty` | Conditional bean registration |

---

## Kubernetes Commands

```bash
# Cluster
kubectl cluster-info
kubectl get nodes

# Pods
kubectl get pods -n <namespace>
kubectl describe pod <pod> -n <namespace>
kubectl logs -f <pod> -n <namespace>
kubectl exec -it <pod> -- /bin/sh

# Deployments
kubectl get deployments
kubectl scale deployment <name> --replicas=3
kubectl rollout status deployment/<name>
kubectl rollout undo deployment/<name>

# Services
kubectl get svc
kubectl port-forward svc/<name> 8080:80

# ConfigMaps & Secrets
kubectl create configmap <name> --from-file=config.yml
kubectl create secret generic <name> --from-literal=key=value

# Debugging
kubectl top pods
kubectl get events --sort-by='.lastTimestamp'
kubectl describe node <node>
```

---

## AWS Services

| Need | Service | Key Feature |
|------|---------|-------------|
| Compute | EC2, ECS, EKS, Lambda | VMs, Containers, Serverless |
| Database (SQL) | RDS, Aurora | Managed PostgreSQL/MySQL |
| Database (NoSQL) | DynamoDB | Key-value, single-digit ms |
| Cache | ElastiCache | Redis / Memcached |
| Queue | SQS | Managed message queue |
| Streaming | Kinesis, MSK | Real-time data streaming |
| Storage | S3 | Object storage, 11 9s durability |
| CDN | CloudFront | Global content delivery |
| API | API Gateway | REST/WebSocket APIs |
| Auth | Cognito | User pools, OAuth2 |
| Monitoring | CloudWatch | Metrics, logs, alarms |
| IaC | CloudFormation, CDK | Infrastructure as Code |
| Container Registry | ECR | Docker image registry |
| DNS | Route 53 | DNS + health checks |
| Load Balancer | ALB, NLB | L7 / L4 load balancing |

---

## SQL vs NoSQL

| Factor | SQL (PostgreSQL) | NoSQL (DynamoDB/Cassandra) |
|--------|-----------------|---------------------------|
| Schema | Fixed, structured | Flexible, schema-less |
| Transactions | ACID | BASE (eventual consistency) |
| Joins | Native | Application-level |
| Scaling | Vertical (+ read replicas) | Horizontal (sharding) |
| Query | Complex SQL | Key-based access |
| Best for | Transactions, relationships | High throughput, flexible schema |

**Decision:** "If you need transactions and complex queries → SQL. If you need scale and flexible schema → NoSQL."

---

## CAP Theorem

```
        Consistency
           /\
          /  \
         /    \
        / CP   \
       /________\
      /   CA     \
     /            \
    /______________\
  Availability    Partition Tolerance
```

- **CP** (Consistency + Partition Tolerance): MongoDB, HBase, Redis Cluster
- **AP** (Availability + Partition Tolerance): Cassandra, DynamoDB, CouchDB
- **CA** (Consistency + Availability): Single-node RDBMS (no partition tolerance)

**In distributed systems, P is mandatory.** So the real choice is **CP vs AP**.

**PACELC extension:** If Partition → choose A or C. Else → choose Latency or Consistency.

---

## Numbers Every Engineer Should Know

| Operation | Time |
|-----------|------|
| L1 cache reference | 0.5 ns |
| L2 cache reference | 7 ns |
| Main memory reference | 100 ns |
| SSD random read | 150 μs |
| HDD seek | 10 ms |
| Network round trip (same DC) | 0.5 ms |
| Network round trip (cross-region) | 150 ms |
| Read 1 MB from memory | 250 μs |
| Read 1 MB from SSD | 1 ms |
| Read 1 MB from network | 10 ms |

### Quick Math
- 1 day = 86,400 seconds ≈ 100K seconds
- 1 million requests/day ≈ 12 QPS
- 1 billion requests/day ≈ 12K QPS
- 1 GB/day ≈ 12 KB/s
