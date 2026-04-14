package com.maplehub.analytics.repository;

import com.maplehub.analytics.model.DashboardQuery;
import com.maplehub.analytics.model.WindowedAggregation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ClickHouse repository — columnar OLAP storage for aggregated analytics.
 *
 * Interview: Why ClickHouse over PostgreSQL for analytics?
 * - Columnar storage: only reads columns needed for query
 * - Vectorized execution: processes data in batches using SIMD
 * - MergeTree engine: automatic partitioning and data ordering
 * - 100x faster for GROUP BY / aggregation queries
 */
@Repository
public class ClickHouseRepository {

    private final JdbcTemplate jdbc;

    public ClickHouseRepository(JdbcTemplate clickHouseJdbcTemplate) {
        this.jdbc = clickHouseJdbcTemplate;
    }

    public void insertAggregation(WindowedAggregation agg) {
        jdbc.update("""
            INSERT INTO event_aggregations
            (tenant_id, event_type, window_start, window_end, event_count, unique_users)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            agg.tenantId(), agg.eventType(),
            Timestamp.from(agg.windowStart()), Timestamp.from(agg.windowEnd()),
            agg.count(), agg.uniqueUsers()
        );
    }

    public List<WindowedAggregation> query(DashboardQuery q) {
        String granularityFn = switch (q.granularity()) {
            case "1m" -> "toStartOfMinute(window_start)";
            case "5m" -> "toStartOfFiveMinutes(window_start)";
            case "1h" -> "toStartOfHour(window_start)";
            case "1d" -> "toStartOfDay(window_start)";
            default -> "toStartOfHour(window_start)";
        };

        String sql = """
            SELECT tenant_id, event_type,
                   %s AS window_start,
                   %s + INTERVAL 1 HOUR AS window_end,
                   sum(event_count) AS event_count,
                   sum(unique_users) AS unique_users
            FROM event_aggregations
            WHERE tenant_id = ? AND window_start >= ? AND window_end <= ?
            """.formatted(granularityFn, granularityFn);

        if (q.eventType() != null && !q.eventType().isBlank()) {
            sql += " AND event_type = '" + q.eventType().replace("'", "") + "'";
        }

        sql += " GROUP BY tenant_id, event_type, window_start, window_end ORDER BY window_start";

        return jdbc.query(sql,
            (rs, _) -> new WindowedAggregation(
                rs.getString("tenant_id"),
                rs.getString("event_type"),
                rs.getTimestamp("window_start").toInstant(),
                rs.getTimestamp("window_end").toInstant(),
                rs.getLong("event_count"),
                rs.getLong("unique_users")
            ),
            q.tenantId(), Timestamp.from(q.from()), Timestamp.from(q.to())
        );
    }

    public List<Map<String, Object>> topEvents(String tenantId, int limit) {
        return jdbc.queryForList("""
            SELECT event_type, sum(event_count) AS total
            FROM event_aggregations
            WHERE tenant_id = ? AND window_start >= now() - INTERVAL 1 HOUR
            GROUP BY event_type ORDER BY total DESC LIMIT ?
            """, tenantId, limit);
    }
}
