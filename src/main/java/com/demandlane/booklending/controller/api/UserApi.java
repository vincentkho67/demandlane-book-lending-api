package com.demandlane.booklending.controller.api;

import java.security.Principal;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.dto.PageResponse;
import com.demandlane.booklending.dto.UserDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users", description = "User management endpoints (Admin only, except /me)")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/users")
public interface UserApi {

    @Operation(
        summary = "Get current user profile",
        description = "Returns the profile of the currently authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/me")
    ResponseEntity<UserDto.Response> me(Principal principal);

    @Operation(
        summary = "Get all users (Admin only)",
        description = "Retrieve a paginated list of all users with optional filtering by name, email, or role"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    @GetMapping
    ResponseEntity<PageResponse<UserDto.Response>> findAll(
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @Parameter(description = "Filter by name, email, or role")
            UserDto.Filter filter);

    @Operation(
        summary = "Get user by ID (Admin only)",
        description = "Retrieve a specific user by their ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/{id}")
    ResponseEntity<UserDto.Response> findById(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id);

    @Operation(
        summary = "Create new user (Admin only)",
        description = "Create a new user account. Default role is MEMBER."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    @PostMapping
    ResponseEntity<UserDto.Response> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User details", required = true)
            @RequestBody UserDto.Request request);

    @Operation(
        summary = "Update user (Admin only)",
        description = "Update user information. Supports partial updates - only provided fields will be updated."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PutMapping("/{id}")
    ResponseEntity<UserDto.Response> update(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user details (partial updates supported)", required = true)
            @RequestBody UserDto.Request request);

    @Operation(
        summary = "Delete user (Admin only)",
        description = "Soft delete a user. The user is marked as deleted but not removed from the database."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id);
}
