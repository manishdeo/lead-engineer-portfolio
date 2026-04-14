package com.maplehub.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderEvents {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderCreatedEvent {
        private String eventId;
        private Long orderId;
        private String customerId;
        private BigDecimal totalAmount;
        private List<OrderItemPayload> items;
        private Instant timestamp;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderConfirmedEvent {
        private Long orderId;
        private String customerId;
        private Instant timestamp;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderCancelledEvent {
        private Long orderId;
        private String customerId;
        private String reason;
        private Instant timestamp;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemPayload {
        private Long productId;
        private int quantity;
        private BigDecimal price;
    }
}
