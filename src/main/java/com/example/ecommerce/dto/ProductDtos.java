package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductRequest(
            @NotBlank @Size(min = 1, max = 255) String name,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @Positive BigDecimal price,
            @NotNull @PositiveOrZero Integer stockQuantity,
            @NotBlank String imageUrl,
            @NotNull Long categoryId) {
    }

    public record ProductResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            Integer stockQuantity,
            String imageUrl,
            Long categoryId,
            String categoryName) {
    }

}
