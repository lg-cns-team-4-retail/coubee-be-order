package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for better error handling and timeout settings
 */
@Configuration
public class FeignConfig {

    /**
     * Configure request timeouts for Feign clients
     * In Kubernetes environment, these timeouts should account for network latency
     * and service startup times
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5000, TimeUnit.MILLISECONDS,  // Connect timeout: 5 seconds
                10000, TimeUnit.MILLISECONDS, // Read timeout: 10 seconds
                true                          // Follow redirects
        );
    }

    /**
     * Configure Feign logging level
     * BASIC level logs request method, URL, response status, and execution time
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Custom error decoder to handle product service errors gracefully
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ProductServiceErrorDecoder();
    }
}
