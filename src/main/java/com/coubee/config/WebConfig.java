package com.coubee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration Class
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:5173}")
    private String[] allowedOrigins;

    /**
     * CORS Configuration
     * Configures Cross-Origin Resource Sharing for the application
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders(
                    "Origin", 
                    "Content-Type", 
                    "Accept", 
                    "Authorization", 
                    "X-User-ID",
                    "X-Requested-With",
                    // PortOne 웹훅 헤더 추가
                    "x-portone-signature",
                    "x-portone-request-timestamp",
                    "User-Agent"
                )
                .exposedHeaders("Location", "X-Total-Count")
                .allowCredentials(true)
                .maxAge(3600);

        // 웹훅 테스트용 - 모든 오리진 허용
        registry.addMapping("/api/payments/webhook")
                .allowedOrigins("*")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        
        // Allow all origins for health check endpoints (for monitoring tools)
        registry.addMapping("/api/health/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        
        // Allow all origins for Swagger UI and API docs
        registry.addMapping("/swagger-ui/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
                
        registry.addMapping("/api-docs/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
} 
