package com.demandlane.booklending.mapper;

import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldMapEntityToResponse() {
        // Given
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("hashedPassword")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        UserDto.Response response = userMapper.toResponse(user);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldMapRequestToEntity() {
        // Given
        UserDto.Request request = UserDto.Request.builder()
                .name("New User")
                .email("new@example.com")
                .password("password123")
                .build();

        // When
        User user = userMapper.toEntity(request);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("New User");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("password123");
        assertThat(user.getRole()).isEqualTo(Role.MEMBER);
        assertThat(user.getId()).isNull(); // Should not map ID
        assertThat(user.getCreatedAt()).isNull(); // Should not map createdAt
        assertThat(user.getUpdatedAt()).isNull(); // Should not map updatedAt
    }

    @Test
    void shouldUpdateEntityFromRequest() {
        // Given
        User existingUser = User.builder()
                .id(1L)
                .name("Old Name")
                .email("old@example.com")
                .password("oldPassword")
                .role(Role.MEMBER)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        UserDto.Request request = UserDto.Request.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .password("newPassword")
                .build();

        // When
        userMapper.updateEntity(existingUser, request);

        // Then
        assertThat(existingUser.getId()).isEqualTo(1L); // ID should not change
        assertThat(existingUser.getName()).isEqualTo("Updated Name");
        assertThat(existingUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(existingUser.getPassword()).isEqualTo("newPassword");
        assertThat(existingUser.getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    void shouldHandleNullEntityGracefully() {
        // When
        UserDto.Response response = userMapper.toResponse(null);

        // Then
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleNullRequestGracefully() {
        // When
        User user = userMapper.toEntity(null);

        // Then
        assertThat(user).isNull();
    }

    @Test
    void shouldPartiallyUpdateEntityIgnoringNullValues() {
        // Given - existing user with all fields populated
        User existingUser = User.builder()
                .id(1L)
                .name("Original Name")
                .email("original@example.com")
                .password("originalPassword")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        // When - update with only name field (other fields are null)
        UserDto.Request partialRequest = UserDto.Request.builder()
                .name("Updated Name Only")
                .email(null)  // Explicitly null
                .password(null)  // Explicitly null
                .build();

        userMapper.updateEntity(existingUser, partialRequest);

        // Then - only name should be updated, other fields should remain unchanged
        assertThat(existingUser.getId()).isEqualTo(1L);
        assertThat(existingUser.getName()).isEqualTo("Updated Name Only"); // Changed
        assertThat(existingUser.getEmail()).isEqualTo("original@example.com"); // Unchanged
        assertThat(existingUser.getPassword()).isEqualTo("originalPassword"); // Unchanged
        assertThat(existingUser.getRole()).isEqualTo(Role.ADMIN); // Unchanged
    }

    @Test
    void shouldPartiallyUpdateOnlyEmailField() {
        // Given
        User existingUser = User.builder()
                .id(1L)
                .name("Original Name")
                .email("original@example.com")
                .password("originalPassword")
                .role(Role.MEMBER)
                .build();

        // When - update only email
        UserDto.Request partialRequest = UserDto.Request.builder()
                .name(null)
                .email("newemail@example.com")
                .password(null)
                .build();

        userMapper.updateEntity(existingUser, partialRequest);

        // Then
        assertThat(existingUser.getName()).isEqualTo("Original Name"); // Unchanged
        assertThat(existingUser.getEmail()).isEqualTo("newemail@example.com"); // Changed
        assertThat(existingUser.getPassword()).isEqualTo("originalPassword"); // Unchanged
        assertThat(existingUser.getRole()).isEqualTo(Role.MEMBER); // Unchanged
    }
}
