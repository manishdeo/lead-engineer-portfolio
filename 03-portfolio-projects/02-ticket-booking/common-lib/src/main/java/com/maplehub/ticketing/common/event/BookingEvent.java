package com.maplehub.ticketing.common.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Immutable domain events for Event Sourcing.
 * Each event represents a state change in the booking lifecycle.
 */
public sealed interface BookingEvent {

    String bookingId();
    Instant timestamp();

    record SeatHeldEvent(
            String bookingId,
            Long showId,
            String customerId,
            List<String> seatIds,
            Instant holdExpiry,
            Instant timestamp
    ) implements BookingEvent {}

    record PaymentInitiatedEvent(
            String bookingId,
            BigDecimal amount,
            String idempotencyKey,
            Instant timestamp
    ) implements BookingEvent {}

    record PaymentCompletedEvent(
            String bookingId,
            String transactionRef,
            BigDecimal amount,
            Instant timestamp
    ) implements BookingEvent {}

    record PaymentFailedEvent(
            String bookingId,
            String reason,
            Instant timestamp
    ) implements BookingEvent {}

    record BookingConfirmedEvent(
            String bookingId,
            Long showId,
            String customerId,
            List<String> seatIds,
            BigDecimal totalAmount,
            String transactionRef,
            Instant timestamp
    ) implements BookingEvent {}

    record BookingCancelledEvent(
            String bookingId,
            String reason,
            Instant timestamp
    ) implements BookingEvent {}

    record SeatReleasedEvent(
            String bookingId,
            Long showId,
            List<String> seatIds,
            String reason,
            Instant timestamp
    ) implements BookingEvent {}
}
