package com.maplehub.ticketing.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_payments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String bookingId;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(unique = true, nullable = false) private String idempotencyKey;
    private String transactionRef;
    private String status;
    private String failureReason;
    @CreationTimestamp private LocalDateTime createdAt;
}
