package com.maplehub.ecommerce.inventory.service;

import com.maplehub.ecommerce.common.event.InventoryEvents.*;
import com.maplehub.ecommerce.common.event.OrderEvents.OrderCreatedEvent;
import com.maplehub.ecommerce.inventory.model.Inventory;
import com.maplehub.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "inventory:lock:";

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Reserving inventory for order: {}", event.getOrderId());

        String lockKey = LOCK_PREFIX + event.getOrderId();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));

        if (Boolean.FALSE.equals(acquired)) {
            log.warn("Could not acquire lock for order: {}", event.getOrderId());
            return;
        }

        try {
            List<ReservedItem> reservedItems = new ArrayList<>();

            for (var item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found in inventory: " + item.getProductId()));

                if (!inventory.hasStock(item.getQuantity())) {
                    // Compensation: release already reserved items
                    releaseReservedItems(reservedItems);

                    kafkaTemplate.send("inventory-events", event.getOrderId().toString(),
                            InventoryReservationFailedEvent.builder()
                                    .orderId(event.getOrderId())
                                    .reason("Insufficient stock for product: " + item.getProductId())
                                    .timestamp(Instant.now())
                                    .build());
                    return;
                }

                inventory.reserve(item.getQuantity());
                inventoryRepository.save(inventory);
                reservedItems.add(ReservedItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build());
            }

            kafkaTemplate.send("inventory-events", event.getOrderId().toString(),
                    InventoryReservedEvent.builder()
                            .orderId(event.getOrderId())
                            .items(reservedItems)
                            .timestamp(Instant.now())
                            .build());

            log.info("Inventory reserved for order: {}", event.getOrderId());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public int getAvailableStock(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        return inventory.getAvailableQuantity() - inventory.getReservedQuantity();
    }

    private void releaseReservedItems(List<ReservedItem> items) {
        for (ReservedItem item : items) {
            inventoryRepository.findByProductId(item.getProductId()).ifPresent(inv -> {
                inv.releaseReservation(item.getQuantity());
                inventoryRepository.save(inv);
            });
        }
    }
}
