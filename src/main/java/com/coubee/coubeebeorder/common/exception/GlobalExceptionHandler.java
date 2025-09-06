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
        
        ApiResponseDto<Object> response = ApiResponseDto.createError("INVALID_STATUS_TRANSITION", ex.getMessage(), errorDetails);
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NotFound.class)
    public ResponseEntity<ApiResponseDto<Object>> handleNotFound(NotFound ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.createError("NOT_FOUND", ex.getMessage(), errorDetails);
                
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.createError("FORBIDDEN", ex.getMessage(), errorDetails);
                
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
        
        ApiResponseDto<Object> response = ApiResponseDto.createError("VALIDATION_FAILED", "Validation failed", errorDetails);
                
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(StoreServiceException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleStoreServiceException(StoreServiceException ex) {
        log.error("Store service communication error: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("cause", "Store Service");

        ApiResponseDto<Object> response = ApiResponseDto.createError(
            "SERVICE_UNAVAILABLE",
            "매장 서비스와의 통신 중 오류가 발생했습니다: " + ex.getMessage(),
            errorDetails
        );
        
        // 503 Service Unavailable 상태 코드를 반환하여 서비스 장애임을 명확히 합니다.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleUserServiceException(UserServiceException ex) {
        log.error("User service communication error: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("cause", "User Service");

        ApiResponseDto<Object> response = ApiResponseDto.createError(
            "SERVICE_UNAVAILABLE",
            "사용자 서비스와의 통신 중 오류가 발생했습니다: " + ex.getMessage(),
            errorDetails
        );
        
        // 503 Service Unavailable 상태 코드를 반환하여 서비스 장애임을 명확히 합니다.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        
        ApiResponseDto<Object> response = ApiResponseDto.createError("INTERNAL_SERVER_ERROR", "An unexpected error occurred", errorDetails);
                
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
