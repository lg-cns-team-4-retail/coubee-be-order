package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * HTTP 헤더를 전파하지 않는 순수한 Feign 클라이언트 설정.
 * 내부 백엔드 API 중 인증 헤더가 필요 없는 경우에 사용합니다.
 */
@Configuration
public class EmptyFeignConfig {

    // 기존 FeignConfig의 타임아웃과 로깅 설정은 그대로 가져옵니다.
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5000, TimeUnit.MILLISECONDS,  // Connect timeout: 5 seconds
                10000, TimeUnit.MILLISECONDS, // Read timeout: 10 seconds
                true                          // Follow redirects
        );
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // ★★★ 디버깅을 위해 로깅 레벨을 FULL로 변경하여 헤더까지 확인
    }
    
    // ★★★ 핵심: 기존 FeignConfig와 달리, 헤더를 추가하는 인터셉터나 에러 디코더 Bean을 여기에 등록하지 않습니다.
    // 이렇게 하면 Spring Cloud가 자동으로 헤더를 전파하는 기본 동작을 무시하게 됩니다.
}