package com.maplehub.ecommerce.payment.controller;

import com.maplehub.ecommerce.payment.model.Payment;
import com.maplehub.ecommerce.payment.service.PaymentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        Payment payment = paymentService.processPayment(
                request.getOrderId(), request.getCustomerId(), request.getAmount(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.refundPayment(orderId));
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PaymentRequest {
        @NotNull private Long orderId;
        @NotBlank private String customerId;
        @NotNull @Positive private BigDecimal amount;
    }
}
