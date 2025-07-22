package com.coubee.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign client configuration class
 */
@Slf4j
@Configuration
public class FeignConfig {

    @Value("${portone.api.secret}")
    private String portOneApiSecret;

    /**
     * Feign logging level configuration
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Feign request interceptor for PortOne API
     * Add authentication headers for PortOne API requests
     */
    @Bean("portOneRequestInterceptor")
    public RequestInterceptor portOneRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Authorization", "PortOne " + portOneApiSecret);
            
            log.debug("Added PortOne authentication header to request: {}", requestTemplate.url());
        };
    }

    /**
     * Feign request interceptor for internal service communication
     * Add common headers for internal service requests
     */
    @Bean("internalServiceRequestInterceptor")
    public RequestInterceptor internalServiceRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("X-Service-Name", "coubee-order-payment-service");
            
            // Add internal service authentication if needed
            // requestTemplate.header("X-Internal-Token", internalServiceToken);
            
            log.debug("Added internal service headers to request: {}", requestTemplate.url());
        };
    }

    /**
     * Feign error decoder
     * Handle errors that occur during Feign client calls
     */
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
