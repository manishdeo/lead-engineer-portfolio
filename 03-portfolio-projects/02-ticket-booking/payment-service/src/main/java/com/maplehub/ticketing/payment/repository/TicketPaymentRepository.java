package com.maplehub.ticketing.payment.repository;

import com.maplehub.ticketing.payment.model.TicketPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketPaymentRepository extends JpaRepository<TicketPayment, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
