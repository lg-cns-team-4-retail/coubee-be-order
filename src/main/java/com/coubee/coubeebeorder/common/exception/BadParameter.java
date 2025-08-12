package com.coubee.coubeebeorder.common.exception;

public class BadParameter extends ClientError {
    public BadParameter(String message) {
        super(message);
        this.errorCode = "BadParameter";
    }
}