package com.coubee.coubeebeorder.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
public class PortOneWebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300; // 5분
    
    @Value("${portone.v2.webhook-secret:default_secret}")
    private String webhookSecret;

        public boolean verifyWebhook(String transactionId, String signature, String timestamp) {
        try {
            if (!isValidTimestamp(timestamp)) {
                log.warn("웹훅 타임스탬프 검증 실패: {}", timestamp);
                return false;
            }

                        String expectedSignature = generateSignature(transactionId, timestamp);
                        String[] signatureParts = signature.split(",");
            if (signatureParts.length != 2) {
                log.warn("[WEBHOOK_DEBUG] Invalid signature format. Expected 'v1,hash'. Received: '{}'", signature);
                return false;
            }

            String receivedSignatureHash = signatureParts[1];
            boolean isValid = verifySignature(receivedSignatureHash, expectedSignature);
            
            if (isValid) {
                log.info("PortOne 웹훅 서명 검증 성공");
            } else {
                                log.warn("PortOne 웹훅 서명 검증 실패");
                log.warn("[WEBHOOK_DEBUG] Expected Signature: '{}'", expectedSignature);
                log.warn("[WEBHOOK_DEBUG] Received Signature: '{}'", signature);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("웹훅 서명 검증 중 오류 발생", e);
            return false;
        }
    }

    private boolean isValidTimestamp(String timestampStr) {
        try {
            long webhookTimestamp = Long.parseLong(timestampStr);
            long currentTimestamp = Instant.now().getEpochSecond();
            long timeDifference = Math.abs(currentTimestamp - webhookTimestamp);
            
            boolean isValid = timeDifference <= TIMESTAMP_TOLERANCE_SECONDS;
            
            if (!isValid) {
                log.warn("타임스탬프 차이가 허용 범위를 초과함: {}초 (허용: {}초)", 
                    timeDifference, TIMESTAMP_TOLERANCE_SECONDS);
            }
            
            return isValid;
            
        } catch (NumberFormatException e) {
            log.warn("잘못된 타임스탬프 형식: {}", timestampStr);
            return false;
        }
    }

        private String generateSignature(String transactionId, String timestamp) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
                String payload = transactionId + "." + timestamp;
        
        String cleanSecret = webhookSecret.startsWith("whsec_") 
            ? webhookSecret.substring(6) 
            : webhookSecret;
        
        byte[] secretBytes = Base64.getDecoder().decode(cleanSecret);
        
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, HMAC_SHA256);
        mac.init(secretKeySpec);
        
        byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    private boolean verifySignature(String receivedSignature, String expectedSignature) {
        if (receivedSignature == null || expectedSignature == null) {
            return false;
        }
        
        return constantTimeEquals(receivedSignature, expectedSignature);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }

    public boolean isWebhookSecretConfigured() {
        return webhookSecret != null 
            && !webhookSecret.trim().isEmpty() 
            && !webhookSecret.equals("test_webhook")
            && !webhookSecret.equals("default_secret");
    }
}