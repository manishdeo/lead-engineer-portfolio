package com.maplehub.ticketing.event.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venues")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String address;
    private int totalCapacity;
}

@Entity
@Table(name = "shows", indexes = {
    @Index(name = "idx_show_date", columnList = "showDate")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class Show {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id") private Venue venue;
    private LocalDateTime showDate;
    @Column(precision = 8, scale = 2) private BigDecimal basePrice;
    private int availableSeats;
    private boolean active = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "show_seats", joinColumns = @JoinColumn(name = "show_id"))
    @Column(name = "seat_id")
    private List<String> seatIds = new ArrayList<>();
}
