package com.example.ecommerce.controller;

import com.example.ecommerce.dto.OrderDtos.OrderResponse;
import com.example.ecommerce.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderResponse checkout() {
        return orderService.checkout();
    }

    @GetMapping
    public Page<OrderResponse> findMyOrders(@PageableDefault(size = 20) Pageable pageable) {
        return orderService.findMyOrders(pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse findMyOrder(@PathVariable Long id) {
        return orderService.findMyOrder(id);
    }
}
