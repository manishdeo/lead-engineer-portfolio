# 📊 Design Real-time Analytics Platform

---

## Requirements
- Ingest 1M+ events/sec
- Real-time dashboards (< 5s latency)
- Historical queries (days/months)
- Aggregations (count, sum, avg, percentiles)
- Multi-tenant, role-based access

## Architecture
```
Data Sources → Kafka (ingestion) → Flink (stream processing) → ClickHouse (OLAP)
                                                                      │
                                                               ┌──────▼──────┐
                                                               │  Grafana /  │
                                                               │  React UI   │
                                                               └─────────────┘
```

### Ingestion Layer
```
SDKs/APIs → API Gateway → Kafka (partitioned by tenant_id)
                            │
                     Schema Registry (Avro/Protobuf)
```

### Processing Layer
```
Kafka → Flink Jobs:
  1. Enrichment (add geo, device info)
  2. Aggregation (1-min, 5-min, 1-hr windows)
  3. Anomaly detection (threshold alerts)
  4. Write to ClickHouse
```

### Storage — ClickHouse (Column-oriented OLAP)
```sql
CREATE TABLE events (
    tenant_id UInt32,
    event_type String,
    user_id String,
    timestamp DateTime,
    properties Map(String, String)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (tenant_id, event_type, timestamp);
```

**Why ClickHouse:** 100x faster than PostgreSQL for analytical queries, columnar compression, real-time inserts.

### Query Layer
- **Real-time:** Direct ClickHouse queries (< 1s for recent data)
- **Pre-aggregated:** Materialized views for common dashboards
- **Historical:** Partition pruning by date range

## Interview Talking Points
1. **Lambda vs Kappa architecture** — Kappa (single stream pipeline) simpler for most cases
2. **ClickHouse vs Druid vs TimescaleDB** — ClickHouse for SQL familiarity + performance
3. **Exactly-once processing** — Flink checkpointing + Kafka transactions
4. **Multi-tenancy** — Partition by tenant, row-level security
5. **Backpressure** — Kafka consumer lag monitoring, auto-scaling Flink
