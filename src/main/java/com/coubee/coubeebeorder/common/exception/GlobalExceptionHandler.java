package com.coubee.coubeebeorder.common.exception;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fromStatus", ex.getFromStatus());
        errorDetails.put("toStatus", ex.getToStatus());
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.builder()
                .success(false)
                .message(ex.getMessage())
                .data(errorDetails)
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NotFound.class)
    public ResponseEntity<ApiResponseDto<Object>> handleNotFound(NotFound ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.builder()
                .success(false)
                .message(ex.getMessage())
                .data(errorDetails)
                .build();
                
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.builder()
                .success(false)
                .message(ex.getMessage())
                .data(errorDetails)
                .build();
                
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("validationErrors", validationErrors);
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.builder()
                .success(false)
                .message("Validation failed")
                .data(errorDetails)
                .build();
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.builder()
                .success(false)
                .message("An unexpected error occurred")
                .data(errorDetails)
                .build();
                
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
