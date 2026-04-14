package com.maplehub.ticketing.booking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class BookingDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HoldRequest {
        @NotNull private Long showId;
        @NotBlank private String customerId;
        @NotEmpty private List<String> seatIds;
        @NotNull @Positive private BigDecimal pricePerSeat;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String bookingId;
        private Long showId;
        private String customerId;
        private List<String> seatIds;
        private BigDecimal totalAmount;
        private String status;
        private Instant holdExpiry;
        private LocalDateTime createdAt;
    }
}
