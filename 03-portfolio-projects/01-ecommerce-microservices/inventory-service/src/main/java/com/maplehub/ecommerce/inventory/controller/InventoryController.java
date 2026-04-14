package com.maplehub.ecommerce.inventory.controller;

import com.maplehub.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}/stock")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable Long productId) {
        int available = inventoryService.getAvailableStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "availableStock", available));
    }
}
