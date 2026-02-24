package com.demandlane.booklending.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat(response.getBody().get("message")).isEqualTo("Resource not found");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    void shouldHandleIllegalStateException() {
        // Given
        IllegalStateException exception = new IllegalStateException("Invalid state");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(500);
        assertThat(response.getBody().get("message")).isEqualTo("Invalid state");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        Exception exception = new Exception("Generic error");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(500);
        assertThat(response.getBody().get("message")).isEqualTo("Generic error");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }
}
