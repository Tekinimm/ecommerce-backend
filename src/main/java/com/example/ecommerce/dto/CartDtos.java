package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(@NotNull Long productId,
                                     @NotNull @Positive Integer quantity) {
    }

    public record UpdateCartItemRequest(@NotNull @Positive Integer quantity) {
    }

    public record CartItemResponse(Long productId, String productName,
                                   BigDecimal unitPrice, Integer quantity, BigDecimal lineTotal) {
    }

    public record CartResponse(Long cartId, Long userId, List<CartItemResponse> items, BigDecimal total) {
    }
}
