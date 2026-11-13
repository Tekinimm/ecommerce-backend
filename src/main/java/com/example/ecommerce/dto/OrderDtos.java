package com.example.ecommerce.dto;

import com.example.ecommerce.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemResponse(Long productId, String productName,
                                    Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record OrderResponse(Long id, Long userId, OrderStatus status,
                                BigDecimal totalAmount, Instant createdAt, List<OrderItemResponse> items) {
    }

    public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
    }
}
