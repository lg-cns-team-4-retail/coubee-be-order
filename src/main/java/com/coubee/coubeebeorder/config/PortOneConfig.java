package com.coubee.coubeebeorder.config;

import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PortOneConfig {
    
    private final PortOneProperties portOneProperties;
    
    @Bean
    public PaymentClient portonePaymentClient() {
        String apiSecret = portOneProperties.getApiSecret();
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("PortOne API Secret is not configured.");
        }
        // 예제 프로젝트와 동일하게 PaymentClient를 생성합니다.
        return new PaymentClient(apiSecret, "https://api.portone.io", null);
    }

    @Bean
    public WebhookVerifier portoneWebhookVerifier() {
        String webhookSecret = portOneProperties.getWebhookSecret(); // PortOneProperties에 webhookSecret 필드 추가 필요
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("PortOne Webhook Secret is not configured.");
        }
        return new WebhookVerifier(webhookSecret);
    }
}