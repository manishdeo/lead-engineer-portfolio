package com.maplehub.ecommerce.payment.service;

import com.maplehub.ecommerce.common.event.PaymentEvents.*;
import com.maplehub.ecommerce.payment.model.Payment;
import com.maplehub.ecommerce.payment.model.PaymentStatus;
import com.maplehub.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Payment processPayment(Long orderId, String customerId, BigDecimal amount, String idempotencyKey) {
        // Idempotency check: return existing payment if already processed
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate payment request detected: idempotencyKey={}", idempotencyKey);
            return existing.get();
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.PENDING)
                .build();

        try {
            // Simulate payment gateway call
            String transactionRef = simulatePaymentGateway(amount);
            payment.setTransactionRef(transactionRef);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment = paymentRepository.save(payment);

            kafkaTemplate.send("payment-events", orderId.toString(),
                    PaymentCompletedEvent.builder()
                            .paymentId(payment.getId())
                            .orderId(orderId)
                            .amount(amount)
                            .transactionRef(transactionRef)
                            .timestamp(Instant.now())
                            .build());

            log.info("Payment completed: orderId={}, txnRef={}", orderId, transactionRef);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            payment = paymentRepository.save(payment);

            kafkaTemplate.send("payment-events", orderId.toString(),
                    PaymentFailedEvent.builder()
                            .orderId(orderId)
                            .reason(e.getMessage())
                            .timestamp(Instant.now())
                            .build());

            log.error("Payment failed: orderId={}", orderId, e);
        }

        return payment;
    }

    @Transactional
    public Payment refundPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.COMPLETED)
                .orElseThrow(() -> new RuntimeException("No completed payment found for order: " + orderId));

        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("Payment refunded: orderId={}", orderId);
        return paymentRepository.save(payment);
    }

    private String simulatePaymentGateway(BigDecimal amount) {
        // Simulate external payment gateway
        if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
            throw new RuntimeException("Amount exceeds limit");
        }
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
