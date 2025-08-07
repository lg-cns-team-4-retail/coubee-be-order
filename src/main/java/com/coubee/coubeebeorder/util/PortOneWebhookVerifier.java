package com.coubee.coubeebeorder.util;

import io.portone.sdk.server.webhook.Webhook;
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

    private final WebhookVerifier webhookVerifier; // ✅ 공식 SDK의 Verifier를 주입받음

    public boolean verifyWebhook(String requestBody, String signature, String timestamp) {
        if (signature == null || timestamp == null) {
            log.warn("Webhook signature or timestamp is null.");
            return false;
        }

        try {
            // ✅ 공식 SDK의 verify 메소드 사용 - 4개 파라미터 필요
            webhookVerifier.verify(requestBody, signature, timestamp, "webhook-id");
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