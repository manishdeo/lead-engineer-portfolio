# 📐 CQRS & Event Sourcing Patterns

---

## CQRS (Command Query Responsibility Segregation)

### Concept
Separate write model (commands) from read model (queries).

```
         ┌─── Command ──→ Write Model ──→ PostgreSQL (normalized)
Client ──┤                      │
         │                      ▼ (event/projection)
         └─── Query ────→ Read Model ───→ Redis/Elasticsearch (denormalized)
```

### When to Use
- Read/write ratio > 10:1
- Different optimization needs for reads vs writes
- Complex domain with multiple read views
- Need to scale reads independently

### When NOT to Use
- Simple CRUD applications
- Team unfamiliar with eventual consistency
- Strong consistency required for reads

### Implementation

```java
// Command Side
@Service
public class OrderCommandService {
    @Transactional
    public void createOrder(CreateOrderCommand cmd) {
        Order order = Order.create(cmd);
        orderRepository.save(order);
        eventPublisher.publish(new OrderCreatedEvent(order));
    }
}

// Query Side — Projection
@Component
public class OrderProjection {
    @KafkaListener(topics = "order-events")
    public void on(OrderCreatedEvent event) {
        OrderView view = OrderView.from(event);
        redisTemplate.opsForValue().set("order:" + event.orderId(), view);
    }
}

// Query Side — Read
@Service
public class OrderQueryService {
    public OrderView getOrder(String orderId) {
        return redisTemplate.opsForValue().get("order:" + orderId);
    }
}
```

---

## Event Sourcing

### Concept
Store all state changes as immutable events. Current state = replay(events).

```
Event Store:
┌────┬──────────────────┬─────────────────────────────┬─────────┐
│ ID │ Aggregate ID      │ Event Type                  │ Version │
├────┼──────────────────┼─────────────────────────────┼─────────┤
│ 1  │ order-123         │ OrderCreated                │ 1       │
│ 2  │ order-123         │ ItemAdded                   │ 2       │
│ 3  │ order-123         │ PaymentProcessed            │ 3       │
│ 4  │ order-123         │ OrderConfirmed              │ 4       │
└────┴──────────────────┴─────────────────────────────┴─────────┘

Current State = fold(events) → { status: CONFIRMED, items: [...], total: 99.99 }
```

### Benefits
- **Complete audit trail** — every change recorded
- **Temporal queries** — "what was the state at time T?"
- **Event replay** — rebuild read models, fix bugs retroactively
- **Debugging** — replay exact sequence of events

### Challenges
- **Eventual consistency** — read model lags behind write
- **Schema evolution** — events are immutable, need versioning
- **Snapshots** — long event streams need periodic snapshots for performance
- **Complexity** — steeper learning curve

### Snapshot Pattern
```
Events 1-1000 → Snapshot at version 1000
New state = Snapshot + replay(events 1001-current)
```

---

## Saga Pattern

### Orchestration (Centralized)
```
Saga Orchestrator:
  1. CreateOrder → Order Service
  2. ReserveInventory → Inventory Service
  3. ProcessPayment → Payment Service
  4. ConfirmOrder → Order Service

Compensation (on failure at step 3):
  3c. ReleaseInventory → Inventory Service
  2c. CancelOrder → Order Service
```

### Choreography (Decentralized)
```
Order Service → OrderCreated event
  → Inventory Service listens → InventoryReserved event
    → Payment Service listens → PaymentProcessed event
      → Order Service listens → OrderConfirmed
```

### Comparison
| Aspect | Orchestration | Choreography |
|--------|--------------|--------------|
| Visibility | Centralized, easy to monitor | Distributed, hard to trace |
| Coupling | Orchestrator knows all services | Services only know events |
| Complexity | Orchestrator can become complex | Cyclic dependencies risk |
| Best for | 3+ services, complex flows | 2 services, simple flows |

---

## 2025 Interview Tips

- CQRS + Event Sourcing are frequently combined but are **independent patterns**
- Mention **Outbox Pattern** when discussing event publishing reliability
- Know when **NOT** to use these patterns (simple CRUD = overkill)
- Reference real implementations: Axon Framework, EventStoreDB, Kafka as event store
