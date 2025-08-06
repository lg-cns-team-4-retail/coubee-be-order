package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignConfig {

    private final PortOneProperties portOneProperties;

    public FeignConfig(PortOneProperties portOneProperties) {
        this.portOneProperties = portOneProperties;
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean("portOneRequestInterceptor")
    public RequestInterceptor portOneRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
            
            // PortOne V2 API는 Bearer 토큰 방식 사용
            String apiSecret = portOneProperties.getApiSecret();
            if (apiSecret != null && !apiSecret.isEmpty()) {
                requestTemplate.header("Authorization", "Bearer " + apiSecret);
                log.debug("Added PortOne Bearer authentication header to request: {}", requestTemplate.url());
            } else {
                log.error("PortOne API Secret is not configured properly!");
                throw new IllegalStateException("PortOne API Secret is required for authentication");
            }
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
                return new RuntimeException("Invalid request. Please check your input values.");
            }
            
            return new RuntimeException("Unexpected error occurred.");
        };
    }
}