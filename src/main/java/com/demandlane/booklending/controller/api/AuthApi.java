package com.demandlane.booklending.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.dto.AuthDto;
import com.demandlane.booklending.dto.UserDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "User registration and login endpoints (Public)")
@RequestMapping("/api/v1/auth")
public interface AuthApi {

    @Operation(
        summary = "Register new user",
        description = "Create a new user account. All new users are registered with MEMBER role by default."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or email already exists", content = @Content)
    })
    @PostMapping("/register")
    ResponseEntity<UserDto.Response> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Registration details (name, email, password)", required = true)
            @RequestBody UserDto.Request request);

    @Operation(
        summary = "Login",
        description = "Authenticate with email and password. Returns a JWT token to use in the Authorization header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful - returns JWT token"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content)
    })
    @PostMapping("/login")
    ResponseEntity<AuthDto.LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login credentials (email and password)", required = true)
            @RequestBody AuthDto.LoginRequest request);
}
