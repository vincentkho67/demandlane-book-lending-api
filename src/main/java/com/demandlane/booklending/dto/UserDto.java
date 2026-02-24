package com.demandlane.booklending.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.demandlane.booklending.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class UserDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User request payload")
    public static class Request {
        @Schema(description = "User's full name", example = "John Doe")
        private String name;

        @Schema(description = "User's email address", example = "john.doe@example.com")
        private String email;

        @Schema(description = "User's password", example = "password123")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String email;
        private Role role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {
        private String name;
        private String email;
        private String role;
    }
}
