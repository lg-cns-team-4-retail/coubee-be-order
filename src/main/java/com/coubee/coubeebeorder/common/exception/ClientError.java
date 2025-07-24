package com.coubee.coubeebeorder.common.exception;

public class ClientError extends RuntimeException {
    private final ApiError apiError;

    public ClientError(ApiError apiError) {
        super(apiError.getMessage());
        this.apiError = apiError;
    }

    public ClientError(String message) {
        super(message);
        this.apiError = ApiError.CLIENT_ERROR;
    }

    public ApiError getApiError() {
        return apiError;
    }
}