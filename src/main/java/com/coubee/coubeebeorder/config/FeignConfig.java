package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor // PortOneProperties를 주입받기 위해 추가
public class FeignConfig {

    private final PortOneProperties portOneProperties;

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean("portOneRequestInterceptor")
    public RequestInterceptor portOneRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
            
            String apiSecret = portOneProperties.getApiSecret();

            if (apiSecret != null) {
                log.info("[SECRET_DEBUG] Loaded API Secret starts with: {}", apiSecret.substring(0, Math.min(apiSecret.length(), 8)));
            } else {
                log.error("[SECRET_DEBUG] API Secret is NULL!");
            }
            
            if (apiSecret == null || apiSecret.trim().isEmpty()) {
                log.error("PortOne API Secret is not configured. Cannot make API calls.");
                throw new IllegalStateException("PortOne API Secret is required for authentication.");
            }
            
            // ✅✅✅ 핵심 수정 부분: "PortOne " -> "Bearer " ✅✅✅
            // PortOne V2 API는 Bearer 토큰 인증 방식을 사용합니다.
            requestTemplate.header("Authorization", "Bearer " + apiSecret);
            
            log.debug("Added PortOne Bearer authentication header to request: {}", requestTemplate.url());
        };
    }

    @Bean("internalServiceRequestInterceptor")
    public RequestInterceptor internalServiceRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("X-Service-Name", "coubee-be-order");
            
            log.debug("Added internal service headers to request: {}", requestTemplate.url());
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign client error: {} - {} - {}", methodKey, response.status(), response.request().url());
            
            if (response.status() >= 500) {
                return new RuntimeException("Server error occurred. Please try again later.");
            } else if (response.status() >= 400) {
                if (response.status() == 401) {
                    return new RuntimeException("Authentication failed. Please check your PortOne API Secret key.");
                }
                return new RuntimeException("Invalid request. Please check your input values.");
            }
            
            return new RuntimeException("Unexpected error occurred.");
        };
    }
}