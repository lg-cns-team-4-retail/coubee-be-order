package com.coubee.coubeebeorder.config;

import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PortOneConfig {

    @Value("${portone.v2.webhook-secret}")
    private String webhookSecret;

    @Bean
    public WebhookVerifier portOneWebhookVerifier() {
        log.info("Initializing PortOne WebhookVerifier with official SDK");
        return new WebhookVerifier(webhookSecret);
    }
}
