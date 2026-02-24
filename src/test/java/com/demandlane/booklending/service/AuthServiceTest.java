package com.demandlane.booklending.service;

import com.demandlane.booklending.dto.AuthDto;
import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.UserMapper;
import com.demandlane.booklending.repository.UserRepository;
import com.demandlane.booklending.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private UserDto.Request userRequest;
    private User user;
    private UserDto.Response userResponse;

    @BeforeEach
    void setUp() {
        userRequest = UserDto.Request.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .build();

        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("hashedPassword")
                .role(Role.MEMBER)
                .build();

        userResponse = UserDto.Response.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    void shouldRegisterNewUser() {
        // Given
        when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.empty());
        when(userMapper.toEntity(any(UserDto.Request.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserDto.Response result = authService.register(userRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");

        verify(userRepository).findActiveByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowException_whenUserAlreadyExists() {
        // Given
        when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authService.register(userRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {
        // Given
        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("jwt-token");

        // When
        AuthDto.LoginResponse result = authService.login(loginRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo("MEMBER");

        verify(userRepository).findActiveByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(jwtUtil).generateToken("test@example.com", "MEMBER");
    }

    @Test
    void shouldThrowException_whenUserNotFound() {
        // Given
        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid email or password");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    void shouldThrowException_whenPasswordDoesNotMatch() {
        // Given
        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email("test@example.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }
}
