package com.maplehub.ticketing.common.exception;

public class TicketingExceptions {

    public static class SeatAlreadyHeldException extends RuntimeException {
        public SeatAlreadyHeldException(String seatId) {
            super("Seat already held: " + seatId);
        }
    }

    public static class SeatNotAvailableException extends RuntimeException {
        public SeatNotAvailableException(String seatId) {
            super("Seat not available: " + seatId);
        }
    }

    public static class BookingNotFoundException extends RuntimeException {
        public BookingNotFoundException(String bookingId) {
            super("Booking not found: " + bookingId);
        }
    }

    public static class BookingExpiredException extends RuntimeException {
        public BookingExpiredException(String bookingId) {
            super("Booking hold expired: " + bookingId);
        }
    }

    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String reason) {
            super("Payment failed: " + reason);
        }
    }
}
