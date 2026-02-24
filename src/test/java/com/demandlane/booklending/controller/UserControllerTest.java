package com.demandlane.booklending.controller;

import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturn403WhenNotAuthenticated() throws Exception {
        // When & Then - Spring Security returns 403 (Forbidden) for unauthenticated requests
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllUsersAsAdmin() throws Exception {
        // Given
        createTestUser("Test User", "test@example.com");

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Test User"))
                .andExpect(jsonPath("$.data[0].email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn403WhenMemberTriesToGetAllUsers() throws Exception {
        // When & Then - MEMBER cannot access admin-only user list
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetUserById() throws Exception {
        // Given
        User user = createTestUser("Test User", "test@example.com");

        // When & Then
        mockMvc.perform(get("/api/v1/users/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenUserNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateUserAsAdmin() throws Exception {
        // Given
        UserDto.Request request = UserDto.Request.builder()
                .name("New User")
                .email("newuser@example.com")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn403WhenMemberTriesToCreate() throws Exception {
        // Given
        UserDto.Request request = UserDto.Request.builder()
                .name("New User")
                .email("newuser@example.com")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserAsAdmin() throws Exception {
        // Given
        User user = createTestUser("Old Name", "old@example.com");

        UserDto.Request request = UserDto.Request.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn403WhenMemberTriesToUpdate() throws Exception {
        // Given
        User user = createTestUser("Test User", "test@example.com");

        UserDto.Request request = UserDto.Request.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPartiallyUpdateUserNameOnly() throws Exception {
        // Given - existing user
        User user = createTestUser("Original Name", "original@example.com");
        String originalPassword = user.getPassword();

        // When - update only name field
        UserDto.Request partialRequest = UserDto.Request.builder()
                .name("Updated Name Only")
                .build(); // email and password are null

        // Then
        mockMvc.perform(put("/api/v1/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Updated Name Only"))
                .andExpect(jsonPath("$.email").value("original@example.com")); // Unchanged

        // Verify password wasn't changed to null
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updatedUser.getPassword()).isEqualTo(originalPassword);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPartiallyUpdateUserEmailOnly() throws Exception {
        // Given
        User user = createTestUser("Original Name", "original@example.com");

        // When - update only email field
        UserDto.Request partialRequest = UserDto.Request.builder()
                .email("newemail@example.com")
                .build(); // name and password are null

        // Then
        mockMvc.perform(put("/api/v1/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Original Name")) // Unchanged
                .andExpect(jsonPath("$.email").value("newemail@example.com")); // Changed
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSoftDeleteUserAsAdmin() throws Exception {
        // Given
        User user = createTestUser("Test User", "test@example.com");

        // When & Then
        mockMvc.perform(delete("/api/v1/users/" + user.getId()))
                .andExpect(status().isNoContent());

        // Verify user is soft deleted
        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deletedUser.isDeleted()).isTrue();
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn403WhenMemberTriesToDelete() throws Exception {
        // Given
        User user = createTestUser("Test User", "test@example.com");

        // When & Then
        mockMvc.perform(delete("/api/v1/users/" + user.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldNotReturnSoftDeletedUsers() throws Exception {
        // Given
        User user = createTestUser("Test User", "test@example.com");
        user.softDelete();
        userRepository.save(user);

        // When & Then
        mockMvc.perform(get("/api/v1/users/" + user.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403OnMeWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "MEMBER")
    void shouldGetOwnProfileAsMember() throws Exception {
        // Given
        createTestUser("Test User", "test@example.com");

        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldGetOwnProfileAsAdmin() throws Exception {
        // Given
        User admin = User.builder()
                .name("Admin User")
                .email("admin@example.com")
                .password("hashedPassword")
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.name").value("Admin User"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllUsersWithoutPaginationParams() throws Exception {
        // Given
        createTestUser("Test User", "test@example.com");

        // When & Then - should use defaults (page=0, size=10) without failing
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldFilterUsersByName() throws Exception {
        // Given
        createTestUser("Alice Smith", "alice@example.com");
        createTestUserWithRole("Bob Jones", "bob@example.com", Role.ADMIN);

        // When & Then
        mockMvc.perform(get("/api/v1/users").param("name", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Alice Smith"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldFilterUsersByEmail() throws Exception {
        // Given
        createTestUser("Alice Smith", "alice@example.com");
        createTestUser("Bob Jones", "bob@example.com");

        // When & Then
        mockMvc.perform(get("/api/v1/users").param("email", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email").value("alice@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldFilterUsersByRole() throws Exception {
        // Given
        createTestUser("Member User", "member@example.com");
        createTestUserWithRole("Admin User", "admin@example.com", Role.ADMIN);

        // When & Then
        mockMvc.perform(get("/api/v1/users").param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].role").value("ADMIN"));
    }

    private User createTestUser(String name, String email) {
        return createTestUserWithRole(name, email, Role.MEMBER);
    }

    private User createTestUserWithRole(String name, String email, Role role) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password("hashedPassword")
                .role(role)
                .build();
        return userRepository.save(user);
    }
}
