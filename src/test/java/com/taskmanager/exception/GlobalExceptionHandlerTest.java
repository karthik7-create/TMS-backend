package com.taskmanager.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ==================== ResourceNotFoundException ====================

    @Test
    @DisplayName("Should return 404 with message for ResourceNotFoundException")
    void handleResourceNotFound_Returns404() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("Task not found with id: 42");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertEquals("Task not found with id: 42", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    // ==================== MethodArgumentNotValidException ====================

    @Test
    @DisplayName("Should return 400 with field errors for validation failures")
    @SuppressWarnings("unchecked")
    void handleValidationErrors_Returns400WithFieldErrors() {
        // Arrange — mock the exception to avoid Spring MethodParameter internals
        FieldError titleError = new FieldError("taskDto", "title", "Title is required");
        FieldError statusError = new FieldError("taskDto", "status", "Status is required");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "taskDto");
        bindingResult.addError(titleError);
        bindingResult.addError(statusError);

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(ex.getMessage()).thenReturn("Validation failed");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Bad Request", body.get("error"));
        assertEquals("Validation failed", body.get("message"));

        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertEquals("Title is required", errors.get("title"));
        assertEquals("Status is required", errors.get("status"));
    }

    // ==================== BadCredentialsException ====================

    @Test
    @DisplayName("Should return 401 for BadCredentialsException")
    void handleBadCredentials_Returns401() {
        // Arrange
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(ex);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
        assertEquals("Invalid email or password", body.get("message"));
    }

    // ==================== IllegalArgumentException ====================

    @Test
    @DisplayName("Should return 400 for IllegalArgumentException")
    void handleIllegalArgument_Returns400() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid task status: INVALID");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Bad Request", body.get("error"));
        assertEquals("Invalid task status: INVALID", body.get("message"));
    }

    // ==================== Generic Exception ====================

    @Test
    @DisplayName("Should return 500 for unexpected exceptions")
    void handleGenericException_Returns500() {
        // Arrange
        Exception ex = new RuntimeException("Something went wrong");

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.get("status"));
        assertEquals("Internal Server Error", body.get("error"));
        assertEquals("An unexpected error occurred. Please try again later.", body.get("message"));
    }

    // ==================== ResourceNotFoundException class ====================

    @Test
    @DisplayName("ResourceNotFoundException should carry correct message")
    void resourceNotFoundException_HasCorrectMessage() {
        // Arrange & Act
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        // Assert
        assertEquals("User not found", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
