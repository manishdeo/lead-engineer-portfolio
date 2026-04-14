package com.maplehub.analytics.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplehub.analytics.config.KafkaConfig;
import com.maplehub.analytics.model.AnalyticsEvent;
import com.maplehub.analytics.model.WindowedAggregation;
import com.maplehub.analytics.repository.ClickHouseRepository;
import com.maplehub.analytics.websocket.DashboardBroadcaster;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Kafka Streams topology: consumes raw events, aggregates in 1-minute tumbling windows,
 * writes to ClickHouse and broadcasts via WebSocket.
 *
 * Interview: Why tumbling windows?
 * - Non-overlapping, fixed-size windows for clean aggregation boundaries
 * - Grace period handles late-arriving events
 * - exactly_once_v2 ensures no double-counting
 */
@Component
public class EventAggregationTopology {

    private static final Logger log = LoggerFactory.getLogger(EventAggregationTopology.class);
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(1);
    private static final Duration GRACE_PERIOD = Duration.ofSeconds(10);

    private final ObjectMapper objectMapper;
    private final ClickHouseRepository repository;
    private final DashboardBroadcaster broadcaster;

    public EventAggregationTopology(ObjectMapper objectMapper,
                                     ClickHouseRepository repository,
                                     DashboardBroadcaster broadcaster) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    @Autowired
    public void buildTopology(StreamsBuilder builder) {
        KStream<String, String> rawEvents = builder.stream(KafkaConfig.RAW_EVENTS_TOPIC);

        // Key by tenantId:eventType for grouped aggregation
        KGroupedStream<String, String> grouped = rawEvents
                .selectKey((k, v) -> extractGroupKey(v))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()));

        // Tumbling window aggregation — count events per window
        KTable<Windowed<String>, Long> windowedCounts = grouped
                .windowedBy(TimeWindows.ofSizeAndGrace(WINDOW_SIZE, GRACE_PERIOD))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("event-counts")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()));

        // Emit aggregation results
        windowedCounts.toStream().foreach((windowedKey, count) -> {
            String[] parts = windowedKey.key().split(":");
            String tenantId = parts[0];
            String eventType = parts.length > 1 ? parts[1] : "unknown";

            var aggregation = new WindowedAggregation(
                    tenantId, eventType,
                    windowedKey.window().startTime(),
                    windowedKey.window().endTime(),
                    count, 0L
            );

            repository.insertAggregation(aggregation);
            broadcaster.broadcast(tenantId, aggregation);
            log.debug("Window closed: {} count={}", windowedKey.key(), count);
        });
    }

    private String extractGroupKey(String eventJson) {
        try {
            AnalyticsEvent event = objectMapper.readValue(eventJson, AnalyticsEvent.class);
            return event.tenantId() + ":" + event.eventType();
        } catch (Exception e) {
            log.warn("Failed to parse event: {}", e.getMessage());
            return "unknown:unknown";
        }
    }
}
