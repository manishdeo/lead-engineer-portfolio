# 🏗️ System Design Mastery

> Complete guide to system design interviews with real-world examples

---

## 📋 Real-World System Designs

### 1. [Design Uber/Ride-Sharing System](./01-uber-system/README.md)
**Complexity:** High  
**Key Concepts:** Real-time matching, Geospatial indexing, WebSocket, Event-driven

**Topics Covered:**
- Real-time driver-rider matching
- Geospatial data structures (QuadTree, Geohash)
- Surge pricing algorithm
- ETA calculation
- Payment processing
- Trip tracking

---

### 2. [Design Netflix/Video Streaming](./02-netflix-system/README.md)
**Complexity:** High  
**Key Concepts:** CDN, Adaptive bitrate streaming, Recommendation engine

**Topics Covered:**
- Video encoding & transcoding
- CDN architecture
- Adaptive bitrate streaming (HLS, DASH)
- Recommendation system
- Content delivery optimization
- Analytics pipeline

---

### 3. [Design Payment System (Stripe-like)](./03-payment-system/README.md)
**Complexity:** Very High  
**Key Concepts:** Distributed transactions, Idempotency, PCI compliance

**Topics Covered:**
- Payment gateway architecture
- Idempotency keys
- Distributed transactions
- Fraud detection
- Reconciliation
- Multi-currency support

---

### 4. [Design Ticket Booking System (BookMyShow)](./04-ticket-booking/README.md)
**Complexity:** High  
**Key Concepts:** Concurrency control, Distributed locking, CQRS

**Topics Covered:**
- Seat selection & locking
- Inventory management
- Distributed locking
- Payment integration
- Cancellation & refunds
- High-traffic handling

---

### 5. [Design Event Management Platform](./05-event-platform/README.md)
**Complexity:** Medium-High  
**Key Concepts:** Event sourcing, CQRS, Saga pattern

**Topics Covered:**
- Event creation & management
- Registration & ticketing
- Notification system
- Check-in system
- Analytics dashboard
- Multi-tenant architecture

---

### 6. [Design ChatGPT-like AI System](./06-chatgpt-system/README.md)
**Complexity:** Very High  
**Key Concepts:** LLM serving, RAG, Vector databases, Streaming

**Topics Covered:**
- LLM inference architecture
- RAG (Retrieval Augmented Generation)
- Vector database integration
- Streaming responses
- Context management
- Cost optimization

---

## 🎯 System Design Approach

### Step 1: Requirements Clarification (5 mins)

**Functional Requirements:**
```
- What are the core features?
- Who are the users?
- What's the expected scale?
```

**Non-Functional Requirements:**
```
- Availability (99.9%, 99.99%?)
- Latency requirements
- Consistency vs Availability trade-offs
- Security requirements
```

**Example Questions to Ask:**
```
✓ How many users?
✓ Read vs Write ratio?
✓ Data retention period?
✓ Geographic distribution?
✓ Peak traffic patterns?
```

---

### Step 2: Capacity Estimation (5 mins)

**Traffic Estimates:**
```
Daily Active Users (DAU): 10M
Requests per day: 100M
Requests per second: 100M / 86400 ≈ 1,200 RPS
Peak traffic (3x): 3,600 RPS
```

**Storage Estimates:**
```
Average data per user: 1KB
Total storage: 10M * 1KB = 10GB
With 3 years retention: 10GB * 365 * 3 ≈ 11TB
With replication (3x): 33TB
```

**Bandwidth Estimates:**
```
Average request size: 10KB
Bandwidth: 1,200 RPS * 10KB = 12 MB/s
Peak bandwidth: 36 MB/s
```

---

### Step 3: High-Level Design (10 mins)

**Components:**
```
┌─────────────┐
│   Clients   │
│ (Web/Mobile)│
└──────┬──────┘
       │
┌──────▼──────────┐
│   Load Balancer │
└──────┬──────────┘
       │
┌──────▼──────────┐
│   API Gateway   │
│  (Rate Limiting)│
└──────┬──────────┘
       │
┌──────▼──────────────────────┐
│     Microservices Layer     │
│  ┌────────┐  ┌────────┐    │
│  │Service1│  │Service2│    │
│  └────────┘  └────────┘    │
└──────┬──────────────────────┘
       │
┌──────▼──────────────────────┐
│      Data Layer             │
│  ┌────────┐  ┌────────┐    │
│  │Database│  │  Cache │    │
│  └────────┘  └────────┘    │
└─────────────────────────────┘
```

---

### Step 4: Deep Dive (15 mins)

**Focus Areas:**
1. **Database Design**
   - Schema design
   - Indexing strategy
   - Sharding approach
   - Replication

2. **Caching Strategy**
   - What to cache?
   - Cache invalidation
   - Cache-aside vs Write-through

3. **Scalability**
   - Horizontal vs Vertical scaling
   - Stateless services
   - Database sharding

4. **Reliability**
   - Fault tolerance
   - Circuit breakers
   - Retry mechanisms
   - Graceful degradation

---

### Step 5: Trade-offs & Bottlenecks (5 mins)

**Common Trade-offs:**
```
✓ Consistency vs Availability (CAP theorem)
✓ Latency vs Throughput
✓ Cost vs Performance
✓ Complexity vs Maintainability
```

**Identify Bottlenecks:**
```
✓ Database becomes bottleneck → Sharding
✓ Network latency → CDN, Caching
✓ Single point of failure → Replication
✓ CPU intensive → Async processing
```

---

## 📊 Common Patterns

### 1. Microservices Architecture

```
API Gateway
    ├── User Service
    ├── Order Service
    ├── Payment Service
    ├── Notification Service
    └── Analytics Service

Each service:
- Independent database
- Independent deployment
- Communicates via REST/gRPC/Events
```

### 2. Event-Driven Architecture

```
Producer → Message Queue → Consumer

Example:
Order Service → Kafka → [Payment Service, Inventory Service, Email Service]
```

### 3. CQRS (Command Query Responsibility Segregation)

```
Write Path:
Client → Command API → Write DB → Event Bus

Read Path:
Client → Query API → Read DB (Materialized View)
```

### 4. Cache Patterns

**Cache-Aside:**
```
1. Check cache
2. If miss, query database
3. Update cache
4. Return data
```

**Write-Through:**
```
1. Write to cache
2. Cache writes to database
3. Return success
```

**Write-Behind:**
```
1. Write to cache
2. Return success
3. Async write to database
```

---

## 🎯 Key Concepts Checklist

### Scalability
- [ ] Load Balancing (Round Robin, Least Connections, Consistent Hashing)
- [ ] Horizontal Scaling
- [ ] Database Sharding
- [ ] Caching (Redis, Memcached)
- [ ] CDN

### Reliability
- [ ] Replication (Master-Slave, Multi-Master)
- [ ] Circuit Breaker
- [ ] Retry with Exponential Backoff
- [ ] Rate Limiting
- [ ] Health Checks

### Performance
- [ ] Database Indexing
- [ ] Query Optimization
- [ ] Async Processing
- [ ] Connection Pooling
- [ ] Compression

### Data Management
- [ ] SQL vs NoSQL
- [ ] Data Partitioning
- [ ] Data Replication
- [ ] Backup & Recovery
- [ ] Data Consistency

### Security
- [ ] Authentication (JWT, OAuth2)
- [ ] Authorization (RBAC, ABAC)
- [ ] Encryption (TLS, At-rest)
- [ ] API Security
- [ ] DDoS Protection

---

## 🔧 Technology Choices

### Databases

**SQL (Relational):**
- PostgreSQL - General purpose, ACID
- MySQL - High read performance
- Use when: ACID required, complex queries

**NoSQL:**
- MongoDB - Document store, flexible schema
- Cassandra - Wide-column, high write throughput
- DynamoDB - Managed, serverless
- Use when: High scalability, flexible schema

**Cache:**
- Redis - In-memory, data structures
- Memcached - Simple key-value

**Search:**
- Elasticsearch - Full-text search, analytics
- Solr - Enterprise search

### Message Queues

- **Kafka** - High throughput, event streaming
- **RabbitMQ** - Reliable message delivery
- **AWS SQS** - Managed queue service
- **Redis Pub/Sub** - Simple pub/sub

### Storage

- **S3** - Object storage
- **HDFS** - Distributed file system
- **MinIO** - Self-hosted object storage

---

## 📈 Scaling Numbers to Remember

```
L1 Cache: 0.5 ns
L2 Cache: 7 ns
RAM: 100 ns
SSD: 150 μs
HDD: 10 ms
Network (same datacenter): 0.5 ms
Network (cross-region): 150 ms

1 MB/s = 8 Mbps
1 GB = 1,000 MB
1 TB = 1,000 GB

1 Million requests/day ≈ 12 requests/second
1 Billion requests/day ≈ 12,000 requests/second
```

---

## 🎓 Interview Tips

1. **Always clarify requirements first**
2. **Think out loud** - explain your reasoning
3. **Start with high-level design** before diving deep
4. **Discuss trade-offs** - there's no perfect solution
5. **Consider scalability** from the start
6. **Mention monitoring & observability**
7. **Be ready to defend your choices**
8. **Ask for feedback** during the interview

---

**Next Steps:**
- Study each system design in detail
- Practice drawing diagrams
- Understand trade-offs
- Review [Design Patterns](./patterns/README.md)

---

**Quick Links:**
- [Uber System](./01-uber-system/README.md)
- [Netflix System](./02-netflix-system/README.md)
- [Payment System](./03-payment-system/README.md)
- [Ticket Booking](./04-ticket-booking/README.md)
