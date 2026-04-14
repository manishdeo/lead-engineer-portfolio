package com.maplehub.ticketing.booking.saga;

import com.maplehub.ticketing.booking.model.Booking;
import com.maplehub.ticketing.booking.model.BookingStatus;
import com.maplehub.ticketing.booking.repository.BookingRepository;
import com.maplehub.ticketing.booking.service.EventStoreService;
import com.maplehub.ticketing.booking.service.SeatLockService;
import com.maplehub.ticketing.common.event.BookingEvent.*;
import com.maplehub.ticketing.common.exception.TicketingExceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Orchestration-based Saga for the booking lifecycle:
 *
 * 1. Hold Seats (Redis lock + SeatHeldEvent)
 * 2. Initiate Payment (PaymentInitiatedEvent → Payment Service)
 * 3. On Payment Success → Confirm Booking (BookingConfirmedEvent)
 * 4. On Payment Failure → Release Seats (SeatReleasedEvent + BookingCancelledEvent)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingSagaOrchestrator {

    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;
    private final EventStoreService eventStoreService;

    /**
     * Handle payment completion — confirm the booking.
     */
    @KafkaListener(topics = "payment-events", groupId = "booking-saga")
    @Transactional
    public void handlePaymentEvent(Object event) {
        if (event instanceof PaymentCompletedEvent completed) {
            handlePaymentCompleted(completed);
        } else if (event instanceof PaymentFailedEvent failed) {
            handlePaymentFailed(failed);
        }
    }

    private void handlePaymentCompleted(PaymentCompletedEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(event.bookingId()));

        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            log.warn("Booking not in PAYMENT_PENDING state: {}", event.bookingId());
            return;
        }

        // Confirm seats in Redis (remove TTL, mark as BOOKED)
        seatLockService.confirmSeats(booking.getShowId(), booking.getSeatIds(), booking.getId());

        // Update booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTransactionRef(event.transactionRef());
        bookingRepository.save(booking);

        // Append event
        eventStoreService.appendEvent(new BookingConfirmedEvent(
                booking.getId(),
                booking.getShowId(),
                booking.getCustomerId(),
                booking.getSeatIds(),
                booking.getTotalAmount(),
                event.transactionRef(),
                Instant.now()
        ));

        log.info("Booking confirmed: {}", booking.getId());
    }

    private void handlePaymentFailed(PaymentFailedEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(event.bookingId()));

        // Compensation: release seats
        seatLockService.releaseSeats(booking.getShowId(), booking.getSeatIds());

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        eventStoreService.appendEvent(new SeatReleasedEvent(
                booking.getId(), booking.getShowId(), booking.getSeatIds(),
                "Payment failed: " + event.reason(), Instant.now()
        ));

        eventStoreService.appendEvent(new BookingCancelledEvent(
                booking.getId(), "Payment failed: " + event.reason(), Instant.now()
        ));

        log.info("Booking cancelled due to payment failure: {}", booking.getId());
    }
}
