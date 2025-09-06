package com.coubee.coubeebeorder.config;

import com.coubee.coubeebeorder.common.exception.StoreServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Store 서비스와의 통신 오류를 명확하게 분류하는 ErrorDecoder
 * HTTP 상태 코드에 따라 적절한 예외를 생성하여 오류 원인 파악을 용이하게 함
 */
@Slf4j
public class StoreClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Store service error: method={}, status={}, reason={}",
                 methodKey, response.status(), response.reason());

        switch (response.status()) {
            case 403:
            case 401:
                return new StoreServiceException("Store service authorization failed.");
            case 404:
                return new StoreServiceException("Could not find the requested resource in Store service.");
            case 500:
            case 503:
                return new StoreServiceException("Store service is currently unavailable. Please try again later.");
            default:
                return new ErrorDecoder.Default().decode(methodKey, response);
        }
    }
}
