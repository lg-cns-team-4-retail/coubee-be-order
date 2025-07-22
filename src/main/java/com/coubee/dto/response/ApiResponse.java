package com.coubee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API Response Wrapper DTO
 */
@Getter
public class ApiResponse<T> {

    @Schema(description = "Success flag", example = "true")
    private final boolean success;

    @Schema(description = "HTTP status code", example = "200")
    private final int status;

    @Schema(description = "Response message", example = "Request processed successfully")
    private final String message;

    @Schema(description = "Response data")
    private final T data;

    private ApiResponse(boolean success, int status, String message, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * Create success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), "Request processed successfully", data);
    }

    /**
     * Create success response with message
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, data);
    }

    /**
     * Create success response with status code and message
     */
    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return new ApiResponse<>(true, status.value(), message, data);
    }

    /**
     * Create error response
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(false, status.value(), message, null);
    }
    
    /**
     * Create error response with data
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(false, status.value(), message, data);
    }
} 
