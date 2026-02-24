package com.demandlane.booklending.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class LoanDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long userId;
        private Long bookId;
        private LocalDateTime borrowedAt;
        private LocalDateTime dueDate;
        private LocalDateTime returnedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long userId;
        private String userName;
        private String userEmail;
        private Long bookId;
        private String bookTitle;
        private String bookAuthor;
        private LocalDateTime borrowedAt;
        private LocalDateTime dueDate;
        private LocalDateTime returnedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {
        private Long userId;
        private Long bookId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BorrowRequest {
        private Long bookId;
    }
}
