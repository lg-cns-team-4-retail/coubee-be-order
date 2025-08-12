package com.coubee.coubeebeorder.common.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientError extends ApiError {
    public ClientError(String message) {
        super(message);
    }
}
