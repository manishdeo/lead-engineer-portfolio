package com.maplehub.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class InventoryEvents {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InventoryReservedEvent {
        private Long orderId;
        private List<ReservedItem> items;
        private Instant timestamp;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InventoryReservationFailedEvent {
        private Long orderId;
        private String reason;
        private Instant timestamp;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReservedItem {
        private Long productId;
        private int quantity;
    }
}
