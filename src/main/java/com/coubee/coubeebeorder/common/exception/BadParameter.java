package com.coubee.coubeebeorder.common.exception;

public class BadParameter extends RuntimeException {
    private final ApiError apiError;

    public BadParameter(ApiError apiError) {
        super(apiError.getMessage());
        this.apiError = apiError;
    }

    public BadParameter(String message) {
        super(message);
        this.apiError = ApiError.BAD_PARAMETER;
    }

    public ApiError getApiError() {
        return apiError;
    }
}