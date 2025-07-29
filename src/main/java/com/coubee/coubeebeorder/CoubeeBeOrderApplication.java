package com.coubee.coubeebeorder;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableDiscoveryClient // Kubernetes에서도 사용되는 동일한 어노테이션
@EnableFeignClients
@EnableJpaAuditing
@EnableKafka
@OpenAPIDefinition(
        info = @Info(
                title = "Coubee Order Service API",
                version = "1.0",
                description = "API Documentation for Order Service"
        )
)
public class CoubeeBeOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoubeeBeOrderApplication.class, args);
    }
}