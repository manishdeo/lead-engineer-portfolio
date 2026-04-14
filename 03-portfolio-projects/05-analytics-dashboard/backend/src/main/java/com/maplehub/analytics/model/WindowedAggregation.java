package com.maplehub.analytics.model;

import java.time.Instant;

public record WindowedAggregation(
    String tenantId,
    String eventType,
    Instant windowStart,
    Instant windowEnd,
    long count,
    long uniqueUsers
) {}
