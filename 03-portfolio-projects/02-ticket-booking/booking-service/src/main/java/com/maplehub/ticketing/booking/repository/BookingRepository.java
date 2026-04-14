package com.maplehub.ticketing.booking.repository;

import com.maplehub.ticketing.booking.model.Booking;
import com.maplehub.ticketing.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Booking> findByStatusAndHoldExpiryBefore(BookingStatus status, Instant now);
}
