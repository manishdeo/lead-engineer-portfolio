package com.maplehub.ticketing.payment.service;

import com.maplehub.ticketing.common.event.BookingEvent.*;
import com.maplehub.ticketing.payment.model.TicketPayment;
import com.maplehub.ticketing.payment.repository.TicketPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPaymentService {

    private final TicketPaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "payment-requests", groupId = "payment-service")
    @Transactional
    public void handlePaymentRequest(Map<String, Object> request) {
        String bookingId = (String) request.get("bookingId");
        String idempotencyKey = (String) request.get("idempotencyKey");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        // Idempotency check
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Duplicate payment request: {}", idempotencyKey);
            return;
        }

        TicketPayment payment = TicketPayment.builder()
                .bookingId(bookingId)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .build();

        try {
            String txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            payment.setTransactionRef(txnRef);
            payment.setStatus("COMPLETED");
            paymentRepository.save(payment);

            kafkaTemplate.send("payment-events", bookingId,
                    new PaymentCompletedEvent(bookingId, txnRef, amount, Instant.now()));

            log.info("Payment completed: bookingId={}, txn={}", bookingId, txnRef);
        } catch (Exception e) {
            payment.setStatus("FAILED");
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            kafkaTemplate.send("payment-events", bookingId,
                    new PaymentFailedEvent(bookingId, e.getMessage(), Instant.now()));

            log.error("Payment failed: bookingId={}", bookingId, e);
        }
    }
}
