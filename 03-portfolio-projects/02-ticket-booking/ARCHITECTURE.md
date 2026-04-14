# 🏗️ Architecture Decisions

## ADR-001: Event Sourcing for Booking State

**Context:** Ticket bookings go through multiple states. Need full audit trail and ability to replay/debug.

**Decision:** Store all booking state changes as immutable events in PostgreSQL + Kafka.

**Event Flow:**
```
SeatHeldEvent → PaymentInitiatedEvent → PaymentCompletedEvent → BookingConfirmedEvent
                                         OR
                                      PaymentFailedEvent → SeatReleasedEvent → BookingCancelledEvent
```

**Benefits:**
- Complete audit trail for disputes
- Temporal queries ("what was the state at time T?")
- Event replay to rebuild read models
- Natural fit for Kafka as event store

---

## ADR-002: CQRS — Separate Read/Write Models

**Context:** Seat availability checks (reads) outnumber bookings (writes) 100:1.

**Decision:**
- **Write model:** PostgreSQL — normalized event store + booking aggregate
- **Read model:** Redis — denormalized seat availability map per show

**Projection:** Kafka consumer materializes events into Redis hash:
```
Key: show:{showId}:seats
Field: seatId
Value: AVAILABLE | HELD:{bookingId} | BOOKED:{bookingId}
```

---

## ADR-003: Redis Distributed Locking for Seat Selection

**Context:** Multiple users may try to book the same seat simultaneously.

**Decision:** Redis SETNX with 10-minute TTL per seat.

**Lock Key:** `seat:lock:{showId}:{seatId}`
**Lock Value:** `{bookingId}:{timestamp}`
**TTL:** 600 seconds (10 minutes)

**Why Redis over DB locks:**
- Sub-millisecond lock acquisition
- Automatic expiry via TTL (no cleanup needed)
- No DB connection pool exhaustion under high concurrency

---

## ADR-004: Orchestration Saga for Booking Flow

**Decision:** Booking Service orchestrates the saga (vs choreography).

**Steps:**
1. **Hold Seats** — Acquire Redis locks, emit SeatHeldEvent
2. **Initiate Payment** — Call Payment Service, emit PaymentInitiatedEvent
3. **Confirm Booking** — On payment success, emit BookingConfirmedEvent
4. **Notify** — Emit NotificationEvent for email/SMS

**Compensation:**
- Payment timeout (30s) → Release seats
- Payment failure → Release seats, emit BookingCancelledEvent
- Seat lock expired during payment → Refund + cancel

---

## ADR-005: WebSocket for Real-time Seat Updates

**Context:** Users need to see seat availability changes in real-time.

**Decision:** STOMP over WebSocket with Redis Pub/Sub for multi-instance broadcasting.

**Flow:**
1. Booking event published to Kafka
2. WebSocket Service consumes event
3. Publishes to Redis Pub/Sub channel `seat-updates:{showId}`
4. All WebSocket Service instances receive and push to connected clients

---

## ADR-006: Idempotent Payment Processing

**Decision:** Client-generated idempotency key = `{bookingId}:{attemptNumber}`

Prevents duplicate charges on network retries or saga retries.
