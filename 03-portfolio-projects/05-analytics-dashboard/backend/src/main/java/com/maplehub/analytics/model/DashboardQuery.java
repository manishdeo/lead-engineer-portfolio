package com.maplehub.analytics.model;

import java.time.Instant;

public record DashboardQuery(
    String tenantId,
    String eventType,
    Instant from,
    Instant to,
    String granularity  // "1m", "5m", "1h", "1d"
) {
    public DashboardQuery {
        if (granularity == null) granularity = "1h";
        if (from == null) from = Instant.now().minusSeconds(86400);
        if (to == null) to = Instant.now();
    }
}
