package com.maplehub.ticketing.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maplehub.ticketing.booking.model.EventStoreEntry;
import com.maplehub.ticketing.booking.repository.EventStoreRepository;
import com.maplehub.ticketing.common.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Append-only event store.
 * Persists events to PostgreSQL and publishes to Kafka for downstream consumers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStoreService {

    private final EventStoreRepository eventStoreRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void appendEvent(BookingEvent event) {
        try {
            int nextVersion = eventStoreRepository.countByAggregateId(event.bookingId()) + 1;

            EventStoreEntry entry = EventStoreEntry.builder()
                    .aggregateId(event.bookingId())
                    .eventType(event.getClass().getSimpleName())
                    .payload(objectMapper.writeValueAsString(event))
                    .version(nextVersion)
                    .timestamp(Instant.now())
                    .build();

            eventStoreRepository.save(entry);

            // Publish to Kafka for CQRS projections and downstream services
            kafkaTemplate.send("booking-events", event.bookingId(), event);

            log.info("Event appended: type={}, bookingId={}, version={}",
                    event.getClass().getSimpleName(), event.bookingId(), nextVersion);
        } catch (Exception e) {
            log.error("Failed to append event for booking: {}", event.bookingId(), e);
            throw new RuntimeException("Event store write failed", e);
        }
    }

    /**
     * Replay all events for a booking to reconstruct state.
     */
    @Transactional(readOnly = true)
    public List<EventStoreEntry> getEvents(String bookingId) {
        return eventStoreRepository.findByAggregateIdOrderByVersionAsc(bookingId);
    }
}
