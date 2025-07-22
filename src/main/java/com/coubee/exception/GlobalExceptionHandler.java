package com.coubee.exception;

import com.coubee.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import feign.FeignException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for handling all application exceptions
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {
    com.coubee.controller.OrderController.class,
    com.coubee.controller.PaymentController.class,
    com.coubee.controller.PaymentWebhookController.class,
    com.coubee.controller.HealthCheckController.class
})
public class GlobalExceptionHandler {

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation error occurred: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Map<String, String>> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST, 
            "Validation failed", 
            errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle PaymentException
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentException(
            PaymentException ex, WebRequest request) {
        
        log.error("Payment error occurred: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle Feign client exceptions
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(
            FeignException ex, WebRequest request) {
        
        log.error("External API call failed: status={}, message={}", 
                 ex.status(), ex.getMessage(), ex);
        
        String message = "External service unavailable";
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        
        if (ex.status() == 400) {
            message = "Invalid request to external service";
            status = HttpStatus.BAD_REQUEST;
        } else if (ex.status() == 404) {
            message = "Resource not found in external service";
            status = HttpStatus.NOT_FOUND;
        } else if (ex.status() == 401 || ex.status() == 403) {
            message = "Authentication failed with external service";
            status = HttpStatus.UNAUTHORIZED;
        }
        
        ApiResponse<Void> response = ApiResponse.error(status, message);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Invalid argument: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.error(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}