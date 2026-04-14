package com.maplehub.ecommerce.order.saga;

import com.maplehub.ecommerce.common.event.InventoryEvents.*;
import com.maplehub.ecommerce.common.event.OrderEvents.*;
import com.maplehub.ecommerce.common.event.PaymentEvents.*;
import com.maplehub.ecommerce.order.model.Order;
import com.maplehub.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestration-based Saga for Order creation flow:
 *
 * 1. Order Created (PENDING) → Publish OrderCreatedEvent
 * 2. Inventory Reserved       → Publish PaymentRequest
 * 3. Payment Completed        → Confirm Order (CONFIRMED)
 *
 * Compensation:
 * - Inventory reservation failed → Cancel Order
 * - Payment failed → Release Inventory + Cancel Order
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Step 1: Initiate saga by publishing order created event
    public void startSaga(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream()
                        .map(item -> OrderItemPayload.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send("order-events", order.getId().toString(), event);
        log.info("Saga started for order: {}", order.getId());
    }

    // Step 2: Inventory reserved → request payment
    @KafkaListener(topics = "inventory-events", groupId = "order-saga",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleInventoryResponse(Object event) {
        if (event instanceof InventoryReservedEvent reserved) {
            log.info("Inventory reserved for order: {}", reserved.getOrderId());
            orderRepository.findById(reserved.getOrderId()).ifPresent(order -> {
                order.setStatus(com.maplehub.ecommerce.order.model.OrderStatus.PAYMENT_PROCESSING);
                orderRepository.save(order);
                // Trigger payment
                kafkaTemplate.send("payment-events", order.getId().toString(), order);
            });
        } else if (event instanceof InventoryReservationFailedEvent failed) {
            log.warn("Inventory reservation failed for order: {}", failed.getOrderId());
            cancelOrder(failed.getOrderId(), failed.getReason());
        }
    }

    // Step 3: Payment completed → confirm order
    @KafkaListener(topics = "payment-events", groupId = "order-saga",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handlePaymentResponse(Object event) {
        if (event instanceof PaymentCompletedEvent completed) {
            log.info("Payment completed for order: {}", completed.getOrderId());
            orderRepository.findById(completed.getOrderId()).ifPresent(order -> {
                order.setStatus(com.maplehub.ecommerce.order.model.OrderStatus.CONFIRMED);
                orderRepository.save(order);

                // Publish confirmation event for notification service
                kafkaTemplate.send("notification-events", OrderConfirmedEvent.builder()
                        .orderId(order.getId())
                        .customerId(order.getCustomerId())
                        .timestamp(Instant.now())
                        .build());
            });
        } else if (event instanceof PaymentFailedEvent failed) {
            log.warn("Payment failed for order: {}", failed.getOrderId());
            // Compensation: release inventory
            kafkaTemplate.send("inventory-events",
                    InventoryReservationFailedEvent.builder()
                            .orderId(failed.getOrderId())
                            .reason("Payment failed - releasing inventory")
                            .timestamp(Instant.now())
                            .build());
            cancelOrder(failed.getOrderId(), failed.getReason());
        }
    }

    private void cancelOrder(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(com.maplehub.ecommerce.order.model.OrderStatus.CANCELLED);
            orderRepository.save(order);
            kafkaTemplate.send("notification-events", OrderCancelledEvent.builder()
                    .orderId(orderId)
                    .customerId(order.getCustomerId())
                    .reason(reason)
                    .timestamp(Instant.now())
                    .build());
            log.info("Order cancelled: id={}, reason={}", orderId, reason);
        });
    }
}
