# 📊 Real-time Analytics Dashboard

> Full-stack real-time analytics platform with Kafka Streams processing, ClickHouse OLAP storage, WebSocket push, and Next.js 15 Server Components dashboard.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Next.js](https://img.shields.io/badge/Next.js-15-black)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

---

## 🎯 Overview

Production-grade analytics platform that ingests high-volume events via Kafka, processes them in real-time with Kafka Streams (tumbling/hopping windows), stores aggregations in ClickHouse, and pushes live updates to a React dashboard via WebSocket.

**Key Interview Differentiators:**
- Kafka Streams stateful processing with windowed aggregations
- ClickHouse columnar OLAP for sub-second analytical queries
- Next.js 15 App Router with Server Components for initial load + WebSocket for live updates
- SSE fallback when WebSocket unavailable
- Multi-tenant with row-level security

---

## 🏗️ Architecture

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  Event SDKs │────▶│    Kafka     │────▶│  Kafka Streams   │────▶│ ClickHouse  │
│  (REST API) │     │  (raw-events)│     │  (aggregation)   │     │  (OLAP)     │
└─────────────┘     └─────────────┘     └────────┬─────────┘     └──────┬──────┘
                                                  │                      │
                                          ┌───────▼───────┐     ┌───────▼──────┐
                                          │  WebSocket     │     │  REST API    │
                                          │  (live push)   │     │  (historical)│
                                          └───────┬───────┘     └───────┬──────┘
                                                  │                      │
                                          ┌───────▼──────────────────────▼──────┐
                                          │       Next.js 15 Dashboard          │
                                          │  (Server Components + Client Charts)│
                                          └─────────────────────────────────────┘
```

### Data Flow
1. **Ingest** — REST API receives events, publishes to Kafka `raw-events` topic
2. **Process** — Kafka Streams aggregates in 1-min tumbling windows (count, sum, avg)
3. **Store** — Aggregated results written to ClickHouse + pushed to `aggregated-events` topic
4. **Push** — WebSocket handler subscribes to `aggregated-events`, broadcasts to connected clients
5. **Query** — Historical queries go directly to ClickHouse via REST API
6. **Render** — Next.js Server Components fetch initial data server-side; client components receive WebSocket updates

---

## 🛠️ Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Event Ingestion | Spring Boot 3.2 + Kafka Producer | High-throughput, schema validation |
| Stream Processing | Kafka Streams | Stateful windowed aggregations, exactly-once |
| OLAP Storage | ClickHouse | 100x faster than PostgreSQL for analytics, columnar compression |
| Real-time Push | Spring WebSocket (STOMP) | Bi-directional, low latency |
| Frontend | Next.js 15, React 18, Recharts | Server Components, streaming SSR |
| Auth | JWT + Role-based | Multi-tenant isolation |

---

## 🚀 Features

- **Real-time streaming** — Sub-second event-to-dashboard latency
- **Windowed aggregations** — 1-min, 5-min, 1-hour tumbling windows
- **Interactive charts** — Line, bar, area, pie with Recharts
- **Server Components** — Initial dashboard rendered server-side for fast FCP
- **WebSocket + SSE fallback** — Graceful degradation
- **Multi-tenant** — Tenant-scoped data with row-level filtering
- **Historical queries** — Date range, group-by, percentiles via ClickHouse
- **Export** — CSV/JSON export of query results
- **Role-based access** — Admin, Analyst, Viewer roles

---

## 📦 Project Structure

```
05-analytics-dashboard/
├── backend/                          # Spring Boot + Kafka Streams
│   ├── pom.xml
│   └── src/main/java/com/maplehub/analytics/
│       ├── AnalyticsApplication.java
│       ├── config/                   # Kafka, ClickHouse, WebSocket config
│       ├── controller/               # REST endpoints
│       ├── model/                    # Event, Aggregation DTOs
│       ├── repository/               # ClickHouse queries
│       ├── service/                  # Business logic
│       ├── stream/                   # Kafka Streams topology
│       └── websocket/                # STOMP WebSocket handler
├── frontend/                         # Next.js 15 App Router
│   ├── package.json
│   └── src/
│       ├── app/                      # App Router pages
│       ├── components/               # Charts, Layout
│       ├── hooks/                    # useWebSocket, useAnalytics
│       └── lib/                      # API client, utils
├── docker-compose.yml
├── kubernetes/
├── .github/workflows/ci.yml
└── scripts/
```

---

## 📊 Performance

| Metric | Target |
|--------|--------|
| Event ingestion | 50K events/sec per partition |
| Dashboard latency | < 2s end-to-end |
| ClickHouse query (recent) | < 100ms |
| ClickHouse query (30 days) | < 1s |
| Frontend FCP | < 1.2s (Server Components) |

---

## 🏃 Getting Started

### Prerequisites
- Java 21, Node.js 20, Docker

### Run Locally
```bash
docker-compose up -d          # Kafka, ClickHouse, Zookeeper
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

### Ingest Test Events
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"t1","eventType":"page_view","userId":"u1","properties":{"page":"/home"}}'
```

---

## 📖 Interview Talking Points

1. **Why Kafka Streams over Flink?** — Embedded in Spring Boot (no separate cluster), sufficient for windowed aggregations, simpler ops
2. **Why ClickHouse?** — Columnar storage, vectorized execution, 100x faster than row-based DBs for GROUP BY queries
3. **Server Components vs Client Components** — Server Components for initial data fetch (no JS bundle), Client Components for interactive charts + WebSocket
4. **Exactly-once semantics** — Kafka Streams `processing.guarantee=exactly_once_v2` + idempotent ClickHouse inserts
5. **Backpressure handling** — Consumer lag monitoring, WebSocket message buffering with drop-oldest policy
6. **Multi-tenancy** — Kafka partitioned by tenant_id, ClickHouse queries filtered by tenant, JWT carries tenant claim

---

## 📄 License

MIT License
