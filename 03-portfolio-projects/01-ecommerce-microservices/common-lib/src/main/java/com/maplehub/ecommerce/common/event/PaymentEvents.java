package com.maplehub.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentEvents {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentCompletedEvent {
        private Long paymentId;
        private Long orderId;
        private BigDecimal amount;
        private String transactionRef;
        private Instant timestamp;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentFailedEvent {
        private Long orderId;
        private String reason;
        private Instant timestamp;
    }
}
