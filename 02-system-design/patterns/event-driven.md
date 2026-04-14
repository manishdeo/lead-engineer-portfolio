# ⚡ Event-Driven Architecture Patterns

---

## Core Concepts

```
Producer → Event Bus (Kafka/SQS) → Consumer(s)
```

### Event Types
| Type | Purpose | Example |
|------|---------|---------|
| **Domain Event** | Something happened | OrderPlaced, PaymentCompleted |
| **Integration Event** | Cross-service communication | OrderCreated → Inventory |
| **Command** | Request to do something | ProcessPayment |
| **Query** | Request for data | GetOrderStatus |

---

## Patterns

### 1. Event Notification
Producer publishes event, doesn't care who consumes.
```
Order Service → "OrderPlaced" → Kafka → Inventory Service
                                      → Notification Service
                                      → Analytics Service
```
✅ Loose coupling. ❌ Hard to trace full flow.

### 2. Event-Carried State Transfer
Event contains full data — consumer doesn't need to call back.
```json
{
  "event": "OrderPlaced",
  "data": {
    "orderId": "123",
    "customerId": "456",
    "items": [...],
    "totalAmount": 99.99
  }
}
```
✅ No callback needed. ❌ Larger messages, data duplication.

### 3. Event Sourcing
Store state changes as immutable events. Replay to reconstruct state.
```
Event Store: [OrderCreated, ItemAdded, ItemAdded, PaymentProcessed, OrderConfirmed]
Current State = replay(events)
```
✅ Full audit trail, temporal queries. ❌ Complexity, eventual consistency.

### 4. Outbox Pattern (Transactional Messaging)
Solve the dual-write problem: DB write + event publish atomically.
```
1. BEGIN TRANSACTION
2. INSERT INTO orders (...)
3. INSERT INTO outbox (event_type, payload)
4. COMMIT
5. Poller/CDC reads outbox → publishes to Kafka
```
Tools: Debezium (CDC), custom poller.

### 5. Saga Pattern
See [CQRS & Event Sourcing](./cqrs-event-sourcing.md) for details.

---

## Kafka Best Practices

### Topic Design
- One topic per event type (not per service)
- Partition key = entity ID (ordering guarantee)
- Retention: 7 days default, longer for event sourcing

### Consumer Groups
```
Topic: order-events (3 partitions)
  Consumer Group A (inventory-service): 3 instances → 1 partition each
  Consumer Group B (notification-service): 2 instances → 1 gets 2 partitions
```

### Exactly-Once Semantics
```
Producer: enable.idempotence=true + transactional.id
Consumer: read_committed isolation + idempotent processing
```

### Dead Letter Queue (DLQ)
```
Consumer fails → retry 3x → DLQ topic → alert → manual review
```

---

## 2025 Trends

- **CloudEvents** — CNCF standard event format (vendor-neutral)
- **AsyncAPI** — OpenAPI equivalent for event-driven APIs
- **Event Mesh** — Multi-protocol event routing (Solace, Confluent)
- **Serverless Events** — EventBridge, Kafka Serverless (Confluent Cloud)
