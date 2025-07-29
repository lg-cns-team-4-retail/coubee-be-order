package com.coubee.coubeebeorder.common.exception;

public class NotFound extends ClientError {
    public NotFound(String message) {
        this.errorCode = "NotFound";
        this.errorMessage = message;
    }
}