package com.coubee.coubeebeorder.config;

import com.coubee.coubeebeorder.common.exception.UserServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * User 서비스와의 통신 오류를 명확하게 분류하는 ErrorDecoder
 * HTTP 상태 코드에 따라 적절한 예외를 생성하여 오류 원인 파악을 용이하게 함
 */
@Slf4j
public class UserClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("User service error: method={}, status={}, reason={}",
                 methodKey, response.status(), response.reason());

        switch (response.status()) {
            case 400:
                return new UserServiceException("Invalid user request format or parameters.");
            case 403:
            case 401:
                return new UserServiceException("User service authorization failed.");
            case 404:
                return new UserServiceException("User not found in User service.");
            case 500:
            case 503:
                return new UserServiceException("User service is currently unavailable. Please try again later.");
            default:
                return new ErrorDecoder.Default().decode(methodKey, response);
        }
    }
}
