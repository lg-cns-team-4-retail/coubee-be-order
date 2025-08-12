package com.coubee.coubeebeorder.config;

import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.common.exception.NotFound;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom error decoder for handling product service errors
 * Converts Feign exceptions to appropriate domain exceptions
 */
@Slf4j
public class ProductServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Product service error: method={}, status={}, reason={}", 
                 methodKey, response.status(), response.reason());

        switch (response.status()) {
            case 404:
                return new NotFound("Product not found");
            case 400:
                return new ApiError("Invalid product request");
            case 500:
                return new ApiError("Product service internal error");
            case 503:
                return new ApiError("Product service temporarily unavailable");
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
