package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartDtos.AddCartItemRequest;
import com.example.ecommerce.dto.CartDtos.CartResponse;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CurrentUser currentUser;

    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, productRepository, currentUser);

        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        cart = Cart.builder().user(user).build();
        cart.setId(10L);

        Category category = Category.builder().name("Electronics").build();
        category.setId(1L);

        product = Product.builder()
                .name("Wireless Headphones")
                .price(new BigDecimal("199.99"))
                .stockQuantity(5)
                .category(category)
                .build();
        product.setId(2L);
    }

    @Test
    void addItemCreatesNewLineAndComputesTotal() {
        when(currentUser.get()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        CartResponse response = cartService.addItem(new AddCartItemRequest(2L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("399.98"));
    }

    @Test
    void addItemTwiceAccumulatesQuantity() {
        when(currentUser.get()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        cartService.addItem(new AddCartItemRequest(2L, 1));
        CartResponse response = cartService.addItem(new AddCartItemRequest(2L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("599.97"));
    }

    @Test
    void addItemRejectsQuantityAboveStock() {
        when(currentUser.get()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(new AddCartItemRequest(2L, 6)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void addItemCreatesCartWhenMissing() {
        when(currentUser.get()).thenReturn(user);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        CartResponse response = cartService.addItem(new AddCartItemRequest(2L, 1));

        assertThat(response.items()).hasSize(1);
    }
}
