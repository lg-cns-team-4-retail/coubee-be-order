package com.coubee.coubeebeorder.advice;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.BadParameter;
import com.coubee.coubeebeorder.common.exception.ClientError;
import com.coubee.coubeebeorder.common.exception.NotFound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiCommonAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation error occurred: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponseDto<Map<String, String>> response = ApiResponseDto.createError(
            "VALIDATION_ERROR", 
            "Validation failed", 
            errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(NotFound.class)
    public ResponseEntity<ApiResponseDto<String>> handleNotFound(NotFound ex) {
        log.warn("Not found error: {}", ex.getMessage());
        
        ApiResponseDto<String> response = ApiResponseDto.createError(
            ex.getApiError().getCode(),
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BadParameter.class)
    public ResponseEntity<ApiResponseDto<String>> handleBadParameter(BadParameter ex) {
        log.warn("Bad parameter error: {}", ex.getMessage());
        
        ApiResponseDto<String> response = ApiResponseDto.createError(
            ex.getApiError().getCode(),
            ex.getMessage()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ClientError.class)
    public ResponseEntity<ApiResponseDto<String>> handleClientError(ClientError ex) {
        log.warn("Client error: {}", ex.getMessage());
        
        ApiResponseDto<String> response = ApiResponseDto.createError(
            ex.getApiError().getCode(),
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<String>> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ApiResponseDto<String> response = ApiResponseDto.createError(
            "SERVER_ERROR",
            "An unexpected error occurred"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}