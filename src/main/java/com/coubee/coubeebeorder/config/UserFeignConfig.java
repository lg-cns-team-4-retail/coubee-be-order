package com.coubee.coubeebeorder.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * User 서비스 전용 Feign 클라이언트 구성
 */
@Configuration
@Import(CommonFeignConfig.class)
public class UserFeignConfig {

    // UserClient만을 위한 에러 디코더
    @Bean
    public ErrorDecoder userErrorDecoder() {
        return new UserClientErrorDecoder();
    }
}
