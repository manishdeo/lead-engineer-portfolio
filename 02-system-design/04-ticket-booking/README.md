# 🎫 Design Ticket Booking System (BookMyShow)

---

## Requirements
- Browse events/shows, view seat maps
- Select and hold seats temporarily (10 min)
- Process payment and confirm booking
- Handle 10K+ concurrent seat selections
- Zero double-bookings guaranteed
- Real-time seat availability updates

## Architecture
```
Client ←WebSocket→ WebSocket Service ← Kafka ← Booking Service
  │                                                    │
  └──REST──→ API Gateway → Booking Service ──→ Payment Service
                               │
                    ┌──────────┼──────────┐
                    │          │          │
              PostgreSQL    Redis      Kafka
              (Event Store) (Locks +   (Event Bus)
                            Read Model)
```

## Key Patterns

### Distributed Seat Locking
```
Redis SETNX seat:lock:{showId}:{seatId} = {bookingId}  TTL=600s
```
- Atomic lock acquisition
- Auto-release via TTL if payment not completed
- No database locks needed

### Event Sourcing
```
SeatHeld → PaymentInitiated → PaymentCompleted → BookingConfirmed
```
- Full audit trail
- Replay to rebuild state
- Debug any booking issue

### CQRS
- Write: PostgreSQL (event store + booking aggregate)
- Read: Redis hash `show:{id}:seats` → `{seatId: AVAILABLE|HELD|BOOKED}`

### Saga (Orchestration)
```
Hold Seats → Pay → Confirm
  ↓ fail      ↓ fail
Release    Release + Refund
```

## Handling Race Conditions
1. Redis SETNX is atomic — only one request wins
2. Optimistic locking on booking version
3. Kafka partition by bookingId — ordered processing
4. Idempotency keys on payment
