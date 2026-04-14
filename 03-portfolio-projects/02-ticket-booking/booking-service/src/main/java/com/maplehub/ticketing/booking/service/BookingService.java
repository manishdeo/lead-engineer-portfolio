package com.maplehub.ticketing.booking.service;

import com.maplehub.ticketing.booking.dto.BookingDto;
import com.maplehub.ticketing.booking.model.Booking;
import com.maplehub.ticketing.booking.model.BookingStatus;
import com.maplehub.ticketing.booking.repository.BookingRepository;
import com.maplehub.ticketing.common.event.BookingEvent.*;
import com.maplehub.ticketing.common.exception.TicketingExceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;
    private final EventStoreService eventStoreService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final long HOLD_MINUTES = 10;

    /**
     * Step 1: Hold seats — acquire distributed locks and create booking.
     */
    @Transactional
    public BookingDto.Response holdSeats(BookingDto.HoldRequest request) {
        String bookingId = UUID.randomUUID().toString();
        Instant holdExpiry = Instant.now().plusSeconds(HOLD_MINUTES * 60);

        // Acquire distributed locks on seats
        seatLockService.lockSeats(request.getShowId(), request.getSeatIds(), bookingId);

        // Calculate total
        BigDecimal total = request.getPricePerSeat()
                .multiply(BigDecimal.valueOf(request.getSeatIds().size()));

        // Create booking aggregate
        Booking booking = Booking.builder()
                .id(bookingId)
                .showId(request.getShowId())
                .customerId(request.getCustomerId())
                .seatIds(request.getSeatIds())
                .totalAmount(total)
                .status(BookingStatus.SEATS_HELD)
                .holdExpiry(holdExpiry)
                .build();

        bookingRepository.save(booking);

        // Append event to event store
        eventStoreService.appendEvent(new SeatHeldEvent(
                bookingId, request.getShowId(), request.getCustomerId(),
                request.getSeatIds(), holdExpiry, Instant.now()
        ));

        log.info("Seats held: bookingId={}, seats={}", bookingId, request.getSeatIds());
        return toResponse(booking);
    }

    /**
     * Step 2: Initiate payment — transition to PAYMENT_PENDING.
     */
    @Transactional
    public BookingDto.Response initiatePayment(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() != BookingStatus.SEATS_HELD) {
            throw new IllegalStateException("Booking not in SEATS_HELD state");
        }

        if (booking.getHoldExpiry().isBefore(Instant.now())) {
            throw new BookingExpiredException(bookingId);
        }

        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);

        String idempotencyKey = bookingId + ":1";
        eventStoreService.appendEvent(new PaymentInitiatedEvent(
                bookingId, booking.getTotalAmount(), idempotencyKey, Instant.now()
        ));

        // Send payment request to Payment Service via Kafka
        kafkaTemplate.send("payment-requests", bookingId, Map.of(
                "bookingId", bookingId,
                "amount", booking.getTotalAmount(),
                "customerId", booking.getCustomerId(),
                "idempotencyKey", idempotencyKey
        ));

        log.info("Payment initiated: bookingId={}, amount={}", bookingId, booking.getTotalAmount());
        return toResponse(booking);
    }

    /**
     * Cancel a booking — release seats.
     */
    @Transactional
    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        seatLockService.releaseSeats(booking.getShowId(), booking.getSeatIds());
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        eventStoreService.appendEvent(new BookingCancelledEvent(
                bookingId, "Cancelled by user", Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public BookingDto.Response getBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        return toResponse(booking);
    }

    /**
     * CQRS Read: Get seat availability from Redis.
     */
    public Map<Object, Object> getSeatAvailability(Long showId) {
        return seatLockService.getSeatAvailability(showId);
    }

    /**
     * Scheduled job: expire stale holds every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void expireStaleHolds() {
        List<Booking> expired = bookingRepository
                .findByStatusAndHoldExpiryBefore(BookingStatus.SEATS_HELD, Instant.now());

        for (Booking booking : expired) {
            seatLockService.releaseSeats(booking.getShowId(), booking.getSeatIds());
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            eventStoreService.appendEvent(new SeatReleasedEvent(
                    booking.getId(), booking.getShowId(), booking.getSeatIds(),
                    "Hold expired", Instant.now()
            ));

            log.info("Booking expired: {}", booking.getId());
        }
    }

    private BookingDto.Response toResponse(Booking booking) {
        return BookingDto.Response.builder()
                .bookingId(booking.getId())
                .showId(booking.getShowId())
                .customerId(booking.getCustomerId())
                .seatIds(booking.getSeatIds())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus().name())
                .holdExpiry(booking.getHoldExpiry())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
