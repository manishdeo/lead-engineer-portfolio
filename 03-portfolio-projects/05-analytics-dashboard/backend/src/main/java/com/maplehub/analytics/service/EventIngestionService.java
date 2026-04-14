package com.maplehub.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplehub.analytics.config.KafkaConfig;
import com.maplehub.analytics.model.AnalyticsEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EventIngestionService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventIngestionService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void ingest(AnalyticsEvent event) {
        String json = serialize(event);
        // Partition by tenantId for ordered processing per tenant
        kafkaTemplate.send(KafkaConfig.RAW_EVENTS_TOPIC, event.tenantId(), json);
    }

    public CompletableFuture<Void> ingestBatch(List<AnalyticsEvent> events) {
        var futures = events.stream()
                .map(e -> kafkaTemplate.send(KafkaConfig.RAW_EVENTS_TOPIC, e.tenantId(), serialize(e)))
                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private String serialize(AnalyticsEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event", e);
        }
    }
}
