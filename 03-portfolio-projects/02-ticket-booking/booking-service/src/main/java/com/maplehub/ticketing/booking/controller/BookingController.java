package com.maplehub.ticketing.booking.controller;

import com.maplehub.ticketing.booking.dto.BookingDto;
import com.maplehub.ticketing.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/hold")
    public ResponseEntity<BookingDto.Response> holdSeats(@Valid @RequestBody BookingDto.HoldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.holdSeats(request));
    }

    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<BookingDto.Response> initiatePayment(@PathVariable String bookingId) {
        return ResponseEntity.ok(bookingService.initiatePayment(bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDto.Response> getBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<Map<Object, Object>> getSeatAvailability(@PathVariable Long showId) {
        return ResponseEntity.ok(bookingService.getSeatAvailability(showId));
    }
}
