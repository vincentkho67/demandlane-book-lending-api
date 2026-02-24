package com.demandlane.booklending.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private final String TEST_NAME = "Test User";
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "hashedPassword";
    private final Role TEST_ROLE = Role.MEMBER;

    private User createTestUser() {
        return User.builder()
                .name(TEST_NAME)
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .role(TEST_ROLE)
                .build();
    }

    @Test
    void shouldSaveAndFindUser() {
        // Given
        User user = createTestUser();

        // When
        userRepository.save(user);
        User foundUser = userRepository.findById(user.getId()).orElse(null);

        // Then
        assertNotNull(foundUser);
        assertEquals(user.getName(), foundUser.getName());
        assertEquals(user.getEmail(), foundUser.getEmail());
        assertEquals(user.getRole(), foundUser.getRole());
    }

    @Test
    void shouldUpdateUser() {
        // Given
        User user = createTestUser();
        userRepository.save(user);

        // When
        user.setName("Updated Name");
        userRepository.save(user);
        User updatedUser = userRepository.findById(user.getId()).orElse(null);

        // Then
        assertNotNull(updatedUser);
        assertEquals("Updated Name", updatedUser.getName());
    }

    @Test
    void shouldDeleteUser() {
        // Given
        User user = createTestUser();
        userRepository.save(user);
        Long userId = user.getId();

        // When
        userRepository.deleteById(userId);

        // Then
        assertEquals(false, userRepository.existsById(userId));
    }

    @Test
    void shouldFindAllUsers() {
        // Given
        long initialCount = userRepository.count();
        User user1 = createTestUser();
        User user2 = createTestUser();
        user2.setEmail("test2@example.com");
        userRepository.save(user1);
        userRepository.save(user2);

        // When
        long count = userRepository.count();

        // Then
        assertEquals(initialCount + 2, count);
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        User user = User.builder()
                .name(TEST_NAME)
                .email("findme@example.com")
                .password(TEST_PASSWORD)
                .role(Role.ADMIN)
                .build();
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findActiveByEmail("findme@example.com");

        // Then
        assertNotNull(found.orElse(null));
        assertEquals("findme@example.com", found.get().getEmail());
        assertEquals(Role.ADMIN, found.get().getRole());
    }

    @Test
    void shouldNotFindDeletedUser() {
        // Given
        User user = createTestUser();
        User saved = userRepository.save(user);
        saved.softDelete();
        userRepository.save(saved);

        // When
        Optional<User> found = userRepository.findActiveByEmail(TEST_EMAIL);

        // Then
        assertEquals(false, found.isPresent());
    }
}
