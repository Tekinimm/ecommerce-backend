package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartDtos.AddCartItemRequest;
import com.example.ecommerce.dto.CartDtos.CartItemResponse;
import com.example.ecommerce.dto.CartDtos.CartResponse;
import com.example.ecommerce.dto.CartDtos.UpdateCartItemRequest;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CurrentUser currentUser;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       CurrentUser currentUser) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public CartResponse getCart() {
        Cart cart = getOrCreateCart(currentUser.get());
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(AddCartItemRequest request) {
        Cart cart = getOrCreateCart(currentUser.get());
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (product.getStockQuantity() < request.quantity()) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = CartItem.builder().cart(cart).product(product).quantity(0).build();
                    cart.getItems().add(newItem);
                    return newItem;
                });
        item.setQuantity(item.getQuantity() + request.quantity());
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(currentUser.get());
        CartItem item = findItem(cart, productId);

        if (request.quantity() > item.getProduct().getStockQuantity()) {
            throw new BadRequestException("Insufficient stock for product: " + item.getProduct().getName());
        }
        item.setQuantity(request.quantity());
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        Cart cart = getOrCreateCart(currentUser.get());
        CartItem item = findItem(cart, productId);
        cart.getItems().remove(item);
        return toResponse(cart);
    }

    @Transactional
    public void clear() {
        Cart cart = getOrCreateCart(currentUser.get());
        cart.getItems().clear();
    }

    Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not in cart: " + productId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal lineTotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            items.add(new CartItemResponse(item.getProduct().getId(), item.getProduct().getName(),
                    item.getProduct().getPrice(), item.getQuantity(), lineTotal));
            total = total.add(lineTotal);
        }
        return new CartResponse(cart.getId(), cart.getUser().getId(), items, total);
    }
}
