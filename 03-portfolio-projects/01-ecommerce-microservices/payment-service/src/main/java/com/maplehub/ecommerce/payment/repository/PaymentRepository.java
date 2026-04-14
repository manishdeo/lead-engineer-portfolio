package com.maplehub.ecommerce.payment.repository;

import com.maplehub.ecommerce.payment.model.Payment;
import com.maplehub.ecommerce.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
