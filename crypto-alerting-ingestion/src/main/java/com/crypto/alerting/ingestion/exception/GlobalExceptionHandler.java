package com.crypto.alerting.ingestion.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides centralized exception handling and standardized error responses.
 */
@RestControllerAdvice
@Slf4j
public final class GlobalExceptionHandler {

    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String STATUS_KEY = "status";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";

    /**
     * Handles exceptions from external service calls.
     *
     * @param ex the WebClientResponseException
     * @return error response entity
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(final WebClientResponseException ex) {
        log.error("External service error: {}", ex.getMessage(), ex);

        final Map<String, Object> errorResponse = createErrorResponse(
                ex.getStatusCode().value(),
                "External Service Error",
                ex.getMessage());

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    /**
     * Handles invalid argument exceptions.
     *
     * @param ex the IllegalArgumentException
     * @return error response entity
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(final IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        final Map<String, Object> errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all other unexpected exceptions.
     *
     * @param ex the Exception
     * @return error response entity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(final Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        final Map<String, Object> errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates a standardized error response map.
     *
     * @param status  HTTP status code
     * @param error   error type
     * @param message error message
     * @return error response map
     */
    private Map<String, Object> createErrorResponse(final int status, final String error, final String message) {
        final Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(TIMESTAMP_KEY, LocalDateTime.now());
        errorResponse.put(STATUS_KEY, status);
        errorResponse.put(ERROR_KEY, error);
        errorResponse.put(MESSAGE_KEY, message);
        return errorResponse;
    }
}
