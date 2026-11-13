package com.example.ecommerce.service;

import com.example.ecommerce.domain.OrderStatus;
import com.example.ecommerce.dto.OrderDtos.OrderItemResponse;
import com.example.ecommerce.dto.OrderDtos.OrderResponse;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private static final Set<OrderStatus> CANCELLABLE = Set.of(OrderStatus.PENDING, OrderStatus.PAID);

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final CurrentUser currentUser;

    public OrderService(OrderRepository orderRepository, CartService cartService, CurrentUser currentUser) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @Transactional
    public OrderResponse checkout() {
        User user = currentUser.get();
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            int quantity = cartItem.getQuantity();
            int stock = cartItem.getProduct().getStockQuantity();
            if (quantity > stock) {
                throw new BadRequestException("Insufficient stock for product: " + cartItem.getProduct().getName());
            }
            BigDecimal unitPrice = cartItem.getProduct().getPrice();
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build());
            cartItem.getProduct().setStockQuantity(stock - quantity);
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        return toResponse(saved);
    }

    public Page<OrderResponse> findMyOrders(Pageable pageable) {
        return orderRepository.findByUserId(currentUser.get().getId(), pageable).map(this::toResponse);
    }

    public Page<OrderResponse> findAllOrders(Pageable pageable) {
        return orderRepository.findAllBy(pageable).map(this::toResponse);
    }

    public OrderResponse findMyOrder(Long orderId) {
        User user = currentUser.get();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != com.example.ecommerce.domain.Role.ADMIN) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (newStatus == OrderStatus.CANCELLED && !CANCELLABLE.contains(order.getStatus())) {
            throw new BadRequestException("Order cannot be cancelled once it is shipped or delivered");
        }
        order.setStatus(newStatus);
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            items.add(new OrderItemResponse(item.getProduct().getId(), item.getProduct().getName(),
                    item.getQuantity(), item.getUnitPrice(), lineTotal));
        }
        return new OrderResponse(order.getId(), order.getUser().getId(), order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt(), items);
    }
}
