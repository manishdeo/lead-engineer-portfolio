package com.maplehub.ticketing.booking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_customer", columnList = "customerId"),
    @Index(name = "idx_booking_show", columnList = "showId"),
    @Index(name = "idx_booking_status", columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Booking {

    @Id
    private String id; // UUID-based booking ID

    @Column(nullable = false)
    private Long showId;

    @Column(nullable = false)
    private String customerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_id")
    private List<String> seatIds = new ArrayList<>();

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    private String transactionRef;

    private Instant holdExpiry;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Version
    private Long version; // Optimistic locking
}
