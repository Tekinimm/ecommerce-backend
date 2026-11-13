package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(@NotBlank String name) {
    }

    public record CategoryResponse(Long id, String name) {
    }
}
