package com.coubee.coubeebeorder.common.exception;

public class NotFound extends ClientError {
    public NotFound(String message) {
        super(message);
        this.errorCode = "NotFound";
    }
}