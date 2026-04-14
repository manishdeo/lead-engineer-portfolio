# 🎫 Event-Driven Ticket Booking System

> Production-ready ticket booking platform (BookMyShow-style) built with Event Sourcing, CQRS, Saga Pattern, and WebSocket for real-time seat updates.

[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 🎯 Overview

A high-concurrency ticket booking system handling **10K+ concurrent seat selections** with zero double-bookings. Demonstrates Event Sourcing, CQRS, distributed locking, and real-time WebSocket updates — patterns frequently asked in Lead/Principal Engineer interviews.

## 🏗️ Architecture

```
                    ┌──────────────┐
                    │   Client     │◄──── WebSocket (real-time seat updates)
                    └──────┬───────┘
                           │ REST
                    ┌──────▼───────┐
                    │  API Gateway │
                    └──────┬───────┘
                           │
         ┌─────────────────┼──────────────────┐
         │                 │                    │
  ┌──────▼──────┐  ┌──────▼───────┐  ┌────────▼────────┐
  │   Event     │  │   Booking    │  │   WebSocket     │
  │  Service    │  │   Service    │  │   Service       │
  │ (Catalog)   │  │ (Saga Orch.) │  │ (Real-time)     │
  └─────────────┘  └──────┬───────┘  └─────────────────┘
                          │                    ▲
                   ┌──────┼──────┐             │
                   │             │             │
            ┌──────▼──────┐ ┌───▼──────────┐  │
            │  Payment    │ │ Notification  │  │
            │  Service    │ │  Service      │  │
            └─────────────┘ └──────────────┘  │
                   │                           │
                   └───────────────────────────┘
                          via Kafka

  ┌─────────────────────────────────────────────┐
  │              Event Store (Kafka)             │
  │  Topics: booking-events, payment-events,    │
  │          seat-events, notification-events    │
  └─────────────────────────────────────────────┘

  ┌──────────┐  ┌──────────┐  ┌──────────────┐
  │PostgreSQL│  │  Redis    │  │ Redis Locks  │
  │(Write DB)│  │(Read/CQRS)│  │(Seat Locking)│
  └──────────┘  └──────────┘  └──────────────┘
```

## 🚀 Features

- **Event Catalog** — Create/manage events, venues, shows, seat maps
- **Real-time Seat Selection** — WebSocket-powered live seat availability
- **Distributed Seat Locking** — Redis-based locks prevent double booking
- **Event Sourcing** — Full audit trail of every booking state change
- **CQRS** — Optimized read model in Redis, write model in PostgreSQL
- **Saga Pattern** — Orchestrated booking flow: Lock → Pay → Confirm
- **Temporary Hold** — 10-minute seat reservation with auto-release
- **Idempotent Payments** — Stripe integration with deduplication
- **Notifications** — Kafka-driven email/SMS on booking events

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Messaging | Apache Kafka (Event Store) |
| Write DB | PostgreSQL |
| Read Model / Cache | Redis |
| Distributed Lock | Redis (Redisson) |
| Real-time | WebSocket (STOMP) |
| Payments | Stripe API (simulated) |
| Observability | Micrometer, Prometheus, Zipkin |
| Containers | Docker, Docker Compose |

## 📦 Project Structure

```
02-ticket-booking/
├── booking-service/        # Core booking + Saga orchestrator + Event Sourcing
├── event-service/          # Event/venue/show catalog
├── payment-service/        # Stripe payment processing
├── notification-service/   # Email/SMS via Kafka
├── websocket-service/      # Real-time seat updates
├── common-lib/             # Shared events, DTOs, exceptions
├── docker-compose.yml
├── pom.xml
└── docs/
```

## ⚡ Quick Start

```bash
# Start infrastructure
docker-compose up -d postgres kafka redis zipkin

# Build
./mvnw clean package -DskipTests

# Run all services
docker-compose up -d
```

| Service | URL |
|---------|-----|
| Event Service | http://localhost:8081 |
| Booking Service | http://localhost:8082 |
| Payment Service | http://localhost:8083 |
| WebSocket | ws://localhost:8084/ws |
| Kafka UI | http://localhost:9090 |

## 📊 Key Design Patterns

### Event Sourcing
Every booking state change is stored as an immutable event:
```
SeatHeldEvent → PaymentInitiatedEvent → PaymentCompletedEvent → BookingConfirmedEvent
```
Replay events to reconstruct any booking's state at any point in time.

### CQRS
```
Write Path: REST → Booking Service → PostgreSQL (Event Store) → Kafka
Read Path:  REST → Booking Service → Redis (Materialized View)
```

### Saga Pattern (Booking Flow)
```
HoldSeats → InitiatePayment → ConfirmBooking → SendNotification
   ↓ fail        ↓ fail            ↓ fail
ReleaseSeats  ReleaseSeats    RefundPayment + ReleaseSeats
```

### Distributed Locking (Seat Selection)
```
1. Client selects seats → Redis SETNX lock per seat (TTL: 10 min)
2. If lock acquired → seats held temporarily
3. Payment completes → lock converted to confirmed booking
4. TTL expires → seats auto-released for others
```

### Race Condition Prevention
- **Optimistic locking** on seat version in PostgreSQL
- **Redis distributed lock** per seat with TTL
- **Idempotency keys** on payment requests
- **Kafka ordering** by bookingId (partition key)

## 📈 Performance

| Metric | Target |
|--------|--------|
| Concurrent seat selections | 10K+ |
| Booking latency (p99) | < 500ms |
| Seat lock acquisition | < 10ms |
| WebSocket update latency | < 50ms |
| Zero double-bookings | ✅ Guaranteed |

## 🎯 Interview Talking Points

1. **Why Event Sourcing?** — Complete audit trail, temporal queries, event replay for debugging
2. **CQRS benefits** — 100:1 read/write ratio optimized, independent scaling
3. **Distributed locking strategy** — Redis SETNX + TTL vs Redisson, handling lock expiry during payment
4. **Race conditions** — How we prevent double-booking with pessimistic + optimistic locking
5. **Saga compensation** — What happens when payment fails mid-booking
6. **WebSocket scaling** — Sticky sessions vs Redis pub/sub for multi-instance
7. **Event replay** — Rebuilding read model from event store
8. **Temporary holds** — TTL-based auto-release vs explicit release

## 📖 Documentation

- [Architecture Decisions](./ARCHITECTURE.md)
- [API Documentation](./docs/api-documentation.md)

## 📄 License

MIT License
