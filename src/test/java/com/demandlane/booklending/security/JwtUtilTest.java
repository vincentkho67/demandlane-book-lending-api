package com.demandlane.booklending.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "testSecretKeyThatIsAtLeast32CharactersLongForHS256AlgorithmTesting");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
    }

    @Test
    void shouldGenerateToken() {
        // When
        String token = jwtUtil.generateToken("test@example.com", "MEMBER");

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void shouldExtractEmailFromToken() {
        // Given
        String token = jwtUtil.generateToken("test@example.com", "MEMBER");

        // When
        String email = jwtUtil.extractEmail(token);

        // Then
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void shouldExtractRoleFromToken() {
        // Given
        String token = jwtUtil.generateToken("test@example.com", "ADMIN");

        // When
        String role = jwtUtil.extractRole(token);

        // Then
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void shouldValidateToken() {
        // Given
        String token = jwtUtil.generateToken("test@example.com", "MEMBER");

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldReturnFalse_whenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsExpired() {
        // Given - Create JwtUtil with very short expiration
        JwtUtil shortExpiryJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpiryJwtUtil, "secret", "testSecretKeyThatIsAtLeast32CharactersLongForHS256AlgorithmTesting");
        ReflectionTestUtils.setField(shortExpiryJwtUtil, "expiration", -1L); // Already expired

        String token = shortExpiryJwtUtil.generateToken("test@example.com", "MEMBER");

        // When
        boolean isValid = shortExpiryJwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // When
        String token1 = jwtUtil.generateToken("user1@example.com", "MEMBER");
        String token2 = jwtUtil.generateToken("user2@example.com", "ADMIN");

        // Then
        assertThat(token1).isNotEqualTo(token2);

        String email1 = jwtUtil.extractEmail(token1);
        String email2 = jwtUtil.extractEmail(token2);
        String role1 = jwtUtil.extractRole(token1);
        String role2 = jwtUtil.extractRole(token2);

        assertThat(email1).isEqualTo("user1@example.com");
        assertThat(email2).isEqualTo("user2@example.com");
        assertThat(role1).isEqualTo("MEMBER");
        assertThat(role2).isEqualTo("ADMIN");
    }
}
