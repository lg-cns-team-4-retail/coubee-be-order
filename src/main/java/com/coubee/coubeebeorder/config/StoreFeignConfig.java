package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Store 서비스 전용 Feign 클라이언트 구성
 */
@Configuration
public class StoreFeignConfig {

    /**
     * Feign 클라이언트의 요청 타임아웃을 구성합니다
     * Kubernetes 환경에서는 네트워크 지연 시간과 서비스 시작 시간을 고려해야 합니다
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(5000, TimeUnit.MILLISECONDS, 10000, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    // StoreClient만을 위한 에러 디코더
    @Bean
    public ErrorDecoder storeErrorDecoder() {
        return new StoreClientErrorDecoder();
    }
}
