package com.coubee.coubeebeorder.common.exception;

public class NotFound extends RuntimeException {
    private final ApiError apiError;

    public NotFound(ApiError apiError) {
        super(apiError.getMessage());
        this.apiError = apiError;
    }

    public NotFound(String message) {
        super(message);
        this.apiError = ApiError.NOT_FOUND;
    }

    public ApiError getApiError() {
        return apiError;
    }
}