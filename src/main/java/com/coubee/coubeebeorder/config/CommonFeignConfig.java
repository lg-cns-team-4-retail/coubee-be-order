package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 모든 Feign 클라이언트가 공유하는 공통 설정
 */
@Configuration
public class CommonFeignConfig {

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
}
