package com.coubee.util;

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

/**
 * PortOne V2 웹훅 서명 검증 유틸리티
 * 
 * PortOne V2에서 보내는 웹훅의 무결성과 출처를 검증합니다.
 * - x-portone-signature 헤더의 서명 검증
 * - x-portone-request-timestamp 헤더의 타임스탬프 검증 (Replay Attack 방지)
 */
@Slf4j
@Component
public class PortOneWebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300; // 5분
    
    @Value("${portone.v2.webhook-secret}")
    private String webhookSecret;

    /**
     * PortOne 웹훅 서명을 검증합니다.
     *
     * @param requestBody 웹훅 요청 본문 (raw body)
     * @param signature x-portone-signature 헤더 값
     * @param timestamp x-portone-request-timestamp 헤더 값
     * @return 검증 성공 시 true, 실패 시 false
     */
    public boolean verifyWebhook(String requestBody, String signature, String timestamp) {
        try {
            // 1. 타임스탬프 검증 (Replay Attack 방지)
            if (!isValidTimestamp(timestamp)) {
                log.warn("웹훅 타임스탬프 검증 실패: {}", timestamp);
                return false;
            }

            // 2. 서명 검증
            String expectedSignature = generateSignature(requestBody, timestamp);
            boolean isValid = verifySignature(signature, expectedSignature);
            
            if (isValid) {
                log.info("PortOne 웹훅 서명 검증 성공");
            } else {
                log.warn("PortOne 웹훅 서명 검증 실패");
                log.debug("Expected: {}, Received: {}", expectedSignature, signature);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("웹훅 서명 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 타임스탬프가 유효한지 검증합니다.
     * 현재 시간과의 차이가 허용 범위 내인지 확인합니다.
     *
     * @param timestampStr 타임스탬프 문자열 (Unix timestamp)
     * @return 유효하면 true, 그렇지 않으면 false
     */
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

    /**
     * PortOne 방식에 따라 서명을 생성합니다.
     * 
     * PortOne V2 서명 생성 방식:
     * HMAC-SHA256(timestamp + "." + requestBody, webhookSecret)
     *
     * @param requestBody 웹훅 요청 본문
     * @param timestamp 타임스탬프
     * @return Base64로 인코딩된 서명
     */
    private String generateSignature(String requestBody, String timestamp) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        // PortOne V2 서명 페이로드: "timestamp.requestBody"
        String payload = timestamp + "." + requestBody;
        
        // 웹훅 시크릿에서 "whsec_" 접두사 제거
        String cleanSecret = webhookSecret.startsWith("whsec_") 
            ? webhookSecret.substring(6) 
            : webhookSecret;
        
        // Base64 디코딩된 시크릿 키 사용
        byte[] secretBytes = Base64.getDecoder().decode(cleanSecret);
        
        // HMAC-SHA256 서명 생성
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, HMAC_SHA256);
        mac.init(secretKeySpec);
        
        byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        
        // Base64로 인코딩하여 반환
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * 서명 값들을 안전하게 비교합니다.
     * 타이밍 공격(Timing Attack)을 방지하기 위해 상수 시간 비교를 사용합니다.
     *
     * @param receivedSignature 받은 서명
     * @param expectedSignature 예상 서명
     * @return 일치하면 true, 그렇지 않으면 false
     */
    private boolean verifySignature(String receivedSignature, String expectedSignature) {
        if (receivedSignature == null || expectedSignature == null) {
            return false;
        }
        
        // 상수 시간 비교 (Timing Attack 방지)
        return constantTimeEquals(receivedSignature, expectedSignature);
    }

    /**
     * 상수 시간 문자열 비교 (Timing Attack 방지)
     *
     * @param a 첫 번째 문자열
     * @param b 두 번째 문자열
     * @return 일치하면 true, 그렇지 않으면 false
     */
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

    /**
     * 웹훅 시크릿이 올바르게 설정되었는지 확인합니다.
     *
     * @return 설정이 유효하면 true, 그렇지 않으면 false
     */
    public boolean isWebhookSecretConfigured() {
        return webhookSecret != null 
            && !webhookSecret.trim().isEmpty() 
            && !webhookSecret.equals("test_webhook")
            && !webhookSecret.equals("여기에_실제_웹훅_ID_입력");
    }
}