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

    public boolean verifyWebhook(String requestBody, String webhookId, String signature, String timestamp) {
        if (signature == null || timestamp == null) {
            log.warn("Webhook signature or timestamp is null.");
            return false;
        }

        try {
            // 디버깅을 위한 상세 로깅
            log.info("[WEBHOOK_DEBUG] Webhook ID: {}", webhookId);
            log.info("[WEBHOOK_DEBUG] Request body length: {}", requestBody.length());
            log.info("[WEBHOOK_DEBUG] Signature: {}", signature);
            log.info("[WEBHOOK_DEBUG] Timestamp: {}", timestamp);
            
            // PortOne V2 JVM SDK의 verify 메소드 사용
            // WebhookVerifier는 생성자에서 이미 시크릿을 받았으므로 여기서는 전달하지 않음
            // verify(msgBody, msgId, msgSignature, msgTimestamp) 순서
            webhookVerifier.verify(
                requestBody,    // msgBody
                webhookId != null ? webhookId : "",  // msgId (webhook-id)
                signature,      // msgSignature  
                timestamp       // msgTimestamp
            );
            log.info("PortOne webhook signature verified successfully.");
            return true;
        } catch (Exception e) {
            log.error("PortOne webhook signature verification failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean isWebhookSecretConfigured() {
        // WebhookVerifier Bean 생성 시점에 null 체크를 하므로, 항상 true로 간주할 수 있음
        return true;
    }
}