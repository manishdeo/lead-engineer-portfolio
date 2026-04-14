CREATE DATABASE IF NOT EXISTS analytics;

-- Raw aggregation table (written by Kafka Streams)
CREATE TABLE IF NOT EXISTS analytics.event_aggregations (
    tenant_id     String,
    event_type    String,
    window_start  DateTime,
    window_end    DateTime,
    event_count   UInt64,
    unique_users  UInt64
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(window_start)
ORDER BY (tenant_id, event_type, window_start)
TTL window_start + INTERVAL 90 DAY;

-- Materialized view: hourly rollup for fast dashboard queries
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.hourly_rollup
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(hour)
ORDER BY (tenant_id, event_type, hour)
AS SELECT
    tenant_id,
    event_type,
    toStartOfHour(window_start) AS hour,
    sum(event_count)  AS event_count,
    sum(unique_users) AS unique_users
FROM analytics.event_aggregations
GROUP BY tenant_id, event_type, hour;

-- Materialized view: daily rollup for historical queries
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.daily_rollup
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(day)
ORDER BY (tenant_id, event_type, day)
AS SELECT
    tenant_id,
    event_type,
    toStartOfDay(window_start) AS day,
    sum(event_count)  AS event_count,
    sum(unique_users) AS unique_users
FROM analytics.event_aggregations
GROUP BY tenant_id, event_type, day;
