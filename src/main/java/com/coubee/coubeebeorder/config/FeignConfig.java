package com.coubee.coubeebeorder.config;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for better error handling and timeout settings
 */
public class FeignConfig {

    /**
     * Configure request timeouts for Feign clients
     * In Kubernetes environment, these timeouts should account for network latency
     * and service startup times
     */
    @Bean("commonRequestOptions")
    public Request.Options requestOptions() {
        return new Request.Options(5000, TimeUnit.MILLISECONDS, 10000, TimeUnit.MILLISECONDS, true);
    }

    @Bean("commonFeignLoggerLevel")
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    // ProductClient만을 위한 에러 디코더이므로 그대로 둡니다.
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ProductServiceErrorDecoder();
    }
}
