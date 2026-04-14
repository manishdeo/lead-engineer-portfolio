package com.maplehub.ecommerce.payment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "orderId"),
    @Index(name = "idx_payment_idempotency", columnList = "idempotencyKey", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    private String transactionRef;

    private String failureReason;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

enum PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}
