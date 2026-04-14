package com.maplehub.ticketing.booking.service;

import com.maplehub.ticketing.common.exception.TicketingExceptions.SeatAlreadyHeldException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis-based distributed seat locking.
 * Uses SETNX with TTL to prevent double-booking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);
    private static final String LOCK_PREFIX = "seat:lock:";
    private static final String SEAT_STATUS_PREFIX = "show:seats:";

    /**
     * Attempt to lock multiple seats atomically.
     * If any seat is already locked, releases all acquired locks and throws.
     */
    public void lockSeats(Long showId, List<String> seatIds, String bookingId) {
        List<String> acquiredLocks = new ArrayList<>();

        try {
            for (String seatId : seatIds) {
                String lockKey = LOCK_PREFIX + showId + ":" + seatId;
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, bookingId, HOLD_DURATION);

                if (Boolean.FALSE.equals(acquired)) {
                    throw new SeatAlreadyHeldException(seatId);
                }
                acquiredLocks.add(lockKey);

                // Update seat status in CQRS read model
                redisTemplate.opsForHash()
                        .put(SEAT_STATUS_PREFIX + showId, seatId, "HELD:" + bookingId);
            }
            log.info("Seats locked: showId={}, seats={}, bookingId={}", showId, seatIds, bookingId);
        } catch (SeatAlreadyHeldException e) {
            // Compensation: release all acquired locks
            releaseLocksInternal(acquiredLocks);
            throw e;
        }
    }

    /**
     * Confirm seats — convert temporary hold to permanent booking.
     */
    public void confirmSeats(Long showId, List<String> seatIds, String bookingId) {
        for (String seatId : seatIds) {
            String lockKey = LOCK_PREFIX + showId + ":" + seatId;
            // Remove TTL lock, update read model to BOOKED
            redisTemplate.delete(lockKey);
            redisTemplate.opsForHash()
                    .put(SEAT_STATUS_PREFIX + showId, seatId, "BOOKED:" + bookingId);
        }
        log.info("Seats confirmed: showId={}, seats={}, bookingId={}", showId, seatIds, bookingId);
    }

    /**
     * Release seats — remove locks and mark as available.
     */
    public void releaseSeats(Long showId, List<String> seatIds) {
        for (String seatId : seatIds) {
            String lockKey = LOCK_PREFIX + showId + ":" + seatId;
            redisTemplate.delete(lockKey);
            redisTemplate.opsForHash()
                    .put(SEAT_STATUS_PREFIX + showId, seatId, "AVAILABLE");
        }
        log.info("Seats released: showId={}, seats={}", showId, seatIds);
    }

    /**
     * Get seat availability for a show (CQRS read model).
     */
    public java.util.Map<Object, Object> getSeatAvailability(Long showId) {
        return redisTemplate.opsForHash().entries(SEAT_STATUS_PREFIX + showId);
    }

    private void releaseLocksInternal(List<String> lockKeys) {
        lockKeys.forEach(redisTemplate::delete);
    }
}
