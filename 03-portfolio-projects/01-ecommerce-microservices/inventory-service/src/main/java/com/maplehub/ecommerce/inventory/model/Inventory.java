package com.maplehub.ecommerce.inventory.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product", columnList = "productId", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Version
    private Long version; // Optimistic locking

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean hasStock(int quantity) {
        return availableQuantity - reservedQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        this.reservedQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
    }

    public void confirmReservation(int quantity) {
        this.reservedQuantity -= quantity;
        this.availableQuantity -= quantity;
    }
}
