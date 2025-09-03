package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
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
     // ★★★ Bean 이름을 명시적으로 지정하여 중복을 피합니다. ★★★
    @Bean("storeClientRequestOptions")
    public Request.Options requestOptions() {
        return new Request.Options(5000, TimeUnit.MILLISECONDS, 10000, TimeUnit.MILLISECONDS, true);
    }

    @Bean("storeClientFeignLoggerLevel")
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    /**
     * ★★★ 핵심 수정: 헤더를 강제로 비우는 RequestInterceptor를 추가합니다. ★★★
     * Spring Cloud의 자동 헤더 전파 로직이 동작하더라도, 이 인터셉터가 실행되는 시점은
     * 실제 HTTP 요청이 나가기 직전이므로 모든 헤더를 확실하게 제거할 수 있습니다.
     * @return a RequestInterceptor that clears all headers.
     */
    @Bean
    public RequestInterceptor clearHeadersInterceptor() {
        return requestTemplate -> {
            // 기존 헤더를 모두 지웁니다.
            requestTemplate.headers(null); 
        };
    }
}