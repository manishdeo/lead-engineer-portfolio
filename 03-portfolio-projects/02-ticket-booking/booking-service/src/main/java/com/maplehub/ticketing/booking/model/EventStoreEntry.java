package com.maplehub.ticketing.booking.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Append-only event store table.
 * Each row is an immutable domain event in the booking lifecycle.
 */
@Entity
@Table(name = "event_store", indexes = {
    @Index(name = "idx_event_aggregate", columnList = "aggregateId"),
    @Index(name = "idx_event_type", columnList = "eventType")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventStoreEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateId; // bookingId

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON serialized event

    @Column(nullable = false)
    private int version; // Aggregate version for ordering

    @Column(nullable = false)
    private Instant timestamp;
}
