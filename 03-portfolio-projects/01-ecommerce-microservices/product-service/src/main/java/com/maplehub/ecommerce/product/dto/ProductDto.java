package com.maplehub.ecommerce.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank private String name;
        private String description;
        @NotBlank private String sku;
        @NotNull @Positive private BigDecimal price;
        @NotBlank private String category;
        private String imageUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private String imageUrl;
        private Boolean active;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String sku;
        private BigDecimal price;
        private String category;
        private String imageUrl;
        private boolean active;
        private LocalDateTime createdAt;
    }
}
