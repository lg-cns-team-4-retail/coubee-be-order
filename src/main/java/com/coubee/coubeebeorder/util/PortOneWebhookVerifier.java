package com.coubee.coubeebeorder.util;

import com.coubee.coubeebeorder.config.PortOneProperties;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneWebhookVerifier {

    private final WebhookVerifier webhookVerifier;
    private final PortOneProperties portOneProperties;

    public boolean verifyWebhook(String requestBody, String signature, String timestamp) {
        if (signature == null || timestamp == null) {
            log.warn("Webhook signature or timestamp is null.");
            return false;
        }

        try {
            String webhookSecret = portOneProperties.getWebhookSecret();
            if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
                log.error("Webhook secret is not configured. Please set portone.v2.webhook-secret in application properties.");
                return false;
            }
            
            // PortOne SDK의 verify 메소드 사용 - 웹훅 시크릿으로 서명 검증
            webhookVerifier.verify(webhookSecret, requestBody, signature, timestamp);
            log.info("PortOne webhook signature verified successfully.");
            return true;
        } catch (Exception e) {
            log.warn("PortOne webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isWebhookSecretConfigured() {
        // WebhookVerifier Bean 생성 시점에 null 체크를 하므로, 항상 true로 간주할 수 있음
        return true;
    }
}