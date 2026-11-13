package com.example.ecommerce.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-1234";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
    }

    @Test
    void generatedTokenContainsEmailAndRole() {
        String token = jwtService.generateToken("user@example.com", "USER");

        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractClaim(token, c -> c.get("role", String.class))).isEqualTo("USER");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.extractEmail(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = new JwtService("another-secret-another-secret-another-12", 60_000)
                .generateToken("user@example.com", "USER");

        assertThatThrownBy(() -> jwtService.extractEmail(token))
                .isInstanceOf(JwtException.class);
    }
}
