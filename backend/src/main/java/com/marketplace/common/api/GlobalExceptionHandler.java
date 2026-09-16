package com.marketplace.common.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> application(ApiException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getError(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR",
                "Please check the highlighted fields.", request.getRequestURI(), fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpServletRequest request) {
        return response(400, "INVALID_REQUEST", "The request contains invalid or unsupported fields.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(HttpServletRequest request) {
        return response(409, "CONFLICT", "This record already exists or conflicts with existing data.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(HttpServletRequest request) {
        return response(403, "FORBIDDEN", "You do not have permission to access this resource.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse error) {
            int status = error.getStatusCode().value();
            return response(status, "REQUEST_ERROR", "The requested operation is unavailable.", request);
        }
        return response(500, "INTERNAL_ERROR", "Something went wrong. Please try again.", request);
    }

    private ResponseEntity<ApiError> response(int status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, error, message, request.getRequestURI()));
    }
}
