package com.coubee;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Entry point for Coubee Order Payment Service application
 * Microservice responsible for order creation, payment processing, and order management
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableKafka
@OpenAPIDefinition(
        info = @Info(
                title = "Coubee Order Payment Service API",
                version = "1.0",
                description = "API Documentation for Order Payment Service"
        )
)
public class OrderPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPaymentServiceApplication.class, args);
    }
} 
