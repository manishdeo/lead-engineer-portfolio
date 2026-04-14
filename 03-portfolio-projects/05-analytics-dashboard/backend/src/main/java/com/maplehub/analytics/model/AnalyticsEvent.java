package com.maplehub.analytics.model;

import java.time.Instant;
import java.util.Map;

public record AnalyticsEvent(
    String tenantId,
    String eventType,
    String userId,
    Instant timestamp,
    Map<String, String> properties
) {
    public AnalyticsEvent {
        if (timestamp == null) timestamp = Instant.now();
    }
}
