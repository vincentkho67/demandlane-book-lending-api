package com.demandlane.booklending.controller;

import com.demandlane.booklending.dto.AuthDto;
import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterNewUserWithMemberRole() throws Exception {

        UserDto.Request request = UserDto.Request.builder()
                .name("New User")
                .email("newuser@example.com")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        // Given - Register a user first
        UserDto.Request registerRequest = UserDto.Request.builder()
                .name("Login User")
                .email("login@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // When - Login with same credentials
        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email("login@example.com")
                .password("password123")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void shouldReturn422WhenRegisteringDuplicateEmail() throws Exception {
        // Given - Register first user
        UserDto.Request firstRequest = UserDto.Request.builder()
                .name("First User")
                .email("duplicate@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        // When - Try to register with same email
        UserDto.Request duplicateRequest = UserDto.Request.builder()
                .name("Duplicate User")
                .email("duplicate@example.com")
                .password("password456")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("User with email duplicate@example.com already exists"));
    }

    @Test
    void shouldReturn404WhenLoginWithNonExistentEmail() throws Exception {
        // Given
        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldReturn422WhenLoginWithWrongPassword() throws Exception {
        // Given - Register a user
        UserDto.Request registerRequest = UserDto.Request.builder()
                .name("Test User")
                .email("testuser@example.com")
                .password("correctPassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // When - Login with wrong password
        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email("testuser@example.com")
                .password("wrongPassword")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
