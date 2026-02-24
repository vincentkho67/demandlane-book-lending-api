package com.demandlane.booklending.service;

import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.UserMapper;
import com.demandlane.booklending.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto.Request userRequest;
    private UserDto.Response userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        userRequest = UserDto.Request.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        userResponse = UserDto.Response.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();
    }

    @Test
    void shouldFindAllUsers() {
        // Given
        Page<User> userPage = new PageImpl<>(List.of(user));
        Pageable pageable = PageRequest.of(0, 10);
        UserDto.Filter filter = new UserDto.Filter();

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        // When
        Page<UserDto.Response> result = userService.findAll(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test User");

        verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable));
        verify(userMapper).toResponse(user);
    }

    @Test
    void shouldFindUserById() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserDto.Response result = userService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test User");

        verify(userRepository).findActiveById(1L);
        verify(userMapper).toResponse(user);
    }

    @Test
    void shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findActiveById(999L);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void shouldSaveUser() {
        // Given
        when(userMapper.toEntity(any(UserDto.Request.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserDto.Response result = userService.save(userRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test User");

        verify(userMapper).toEntity(userRequest);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void shouldUpdateUser() {
        // Given
        UserDto.Request updateRequest = UserDto.Request.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        User updatedUser = User.builder()
                .id(1L)
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        UserDto.Response updatedResponse = UserDto.Response.builder()
                .id(1L)
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.of(user));
        doNothing().when(userMapper).updateEntity(any(User.class), any(UserDto.Request.class));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(updatedResponse);

        // When
        UserDto.Response result = userService.update(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");

        verify(userRepository).findActiveById(1L);
        verify(userMapper).updateEntity(user, updateRequest);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(updatedUser);
    }

    @Test
    void shouldThrowException_whenUpdatingNonExistentUser() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.update(999L, userRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findActiveById(999L);
        verify(userMapper, never()).updateEntity(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldSoftDeleteUser() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.delete(1L);

        // Then
        verify(userRepository).findActiveById(1L);
        verify(userRepository).save(user);
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldThrowException_whenDeletingNonExistentUser() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findActiveById(999L);
        verify(userRepository, never()).save(any());
    }
}
