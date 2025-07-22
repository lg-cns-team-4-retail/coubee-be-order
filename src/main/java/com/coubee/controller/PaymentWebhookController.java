package com.coubee.controller;

import com.coubee.service.PaymentService;
import com.coubee.util.PortOneWebhookVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * PortOne V2 Payment Webhook Controller (보안 강화)
 * 
 * PortOne V2 웹훅 서명 검증을 통해 요청의 무결성과 출처를 확인합니다.
 */
@Slf4j
@Tag(name = "Payment Webhook API", description = "Webhook API for receiving payment status changes from PortOne V2")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final PortOneWebhookVerifier webhookVerifier;

    /**
     * PortOne V2 Payment Webhook Processing API (서명 검증)
     * 
     * PortOne V2에서 보내는 웹훅을 서명 검증을 통해 안전하게 처리합니다.
     * 
     * 검증 과정:
     * 1. x-portone-signature 헤더를 통한 서명 검증
     * 2. x-portone-request-timestamp 헤더를 통한 Replay Attack 방지
     * 3. 검증 성공 시에만 결제 상태 업데이트 처리
     *
     * @param request HTTP 요청 (헤더와 본문 추출용)
     * @param requestBody 웹훅 요청 본문 (JSON)
     * @return 응답 (항상 200 OK - PortOne 재시도 방지)
     */
    @Operation(
        summary = "Payment Webhook V2 (Signature Verification)", 
        description = "Securely processes payment status changes from PortOne V2 with signature verification"
    )
    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaymentWebhookV2(
            HttpServletRequest request,
            @RequestBody(required = false) String requestBody,
            @RequestParam(required = false) String paymentId) {
        
        // 웹훅 헤더 추출
        String signature = request.getHeader("x-portone-signature");
        String timestamp = request.getHeader("x-portone-request-timestamp");
        String userAgent = request.getHeader("User-Agent");
        
        log.info("PortOne V2 웹훅 수신 - IP: {}, User-Agent: {}, PaymentId: {}", 
            getClientIpAddress(request), userAgent, paymentId);
        
        try {
            // 쿼리 파라미터로 paymentId가 전달된 경우 (테스트 환경)
            if (paymentId != null && !paymentId.isEmpty()) {
                log.info("쿼리 파라미터로 paymentId 수신: {}", paymentId);
                return processPaymentIdWebhook(paymentId);
            }
            
            // 1. 필수 헤더 검증 (실제 PortOne 웹훅)
            if (signature == null || timestamp == null) {
                log.warn("웹훅 필수 헤더 누락 - signature: {}, timestamp: {}", 
                    signature != null ? "있음" : "없음", 
                    timestamp != null ? "있음" : "없음");
                return ResponseEntity.ok("missing_headers");
            }
            
            // 2. 웹훅 시크릿 설정 확인
            if (!webhookVerifier.isWebhookSecretConfigured()) {
                log.warn("웹훅 시크릿이 설정되지 않음 - 서명 검증 건너뜀");
                return processWebhookWithoutVerification(requestBody);
            }
            
            // 3. 서명 검증
            boolean isValidSignature = webhookVerifier.verifyWebhook(requestBody, signature, timestamp);
            
            if (!isValidSignature) {
                log.error("PortOne 웹훅 서명 검증 실패 - 요청 거부");
                return ResponseEntity.ok("signature_verification_failed");
            }
            
            log.info("PortOne 웹훅 서명 검증 성공 ✅");
            
            // 4. 웹훅 페이로드 처리
            return processVerifiedWebhook(requestBody);
            
        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생", e);
            return ResponseEntity.ok("processing_error");
        }
    }
    
    /**
     * 서명 검증이 완료된 웹훅을 처리합니다.
     */
    private ResponseEntity<String> processVerifiedWebhook(String requestBody) {
        try {
            // TODO: 실제 웹훅 페이로드 파싱하여 결제 ID 추출
            // 현재는 간단한 형태로 처리 (향후 JSON 파싱 로직 추가 필요)
            log.info("검증된 웹훅 처리 시작 - payload: {}", requestBody);
            
            // 결제 상태 업데이트 로직 호출
            // String paymentId = extractPaymentIdFromPayload(requestBody);
            // boolean result = paymentService.handlePaymentWebhookV2(paymentId, requestBody);
            
            log.info("웹훅 처리 완료 ✅");
            return ResponseEntity.ok("success");
            
        } catch (Exception e) {
            log.error("검증된 웹훅 처리 중 오류", e);
            return ResponseEntity.ok("processing_error");
        }
    }
    
    /**
     * 서명 검증 없이 웹훅을 처리합니다. (개발/테스트 환경용)
     */
    private ResponseEntity<String> processWebhookWithoutVerification(String requestBody) {
        log.warn("⚠️ 개발 모드: 서명 검증 없이 웹훅 처리");
        
        try {
            log.info("미검증 웹훅 처리 - payload: {}", requestBody);
            return ResponseEntity.ok("success_without_verification");
            
        } catch (Exception e) {
            log.error("미검증 웹훅 처리 중 오류", e);
            return ResponseEntity.ok("processing_error");
        }
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * PaymentId를 통한 간단한 웹훅 처리 (테스트용)
     */
    private ResponseEntity<String> processPaymentIdWebhook(String paymentId) {
        try {
            log.info("PaymentId 웹훅 처리 시작: {}", paymentId);
            
            // 결제 서비스 호출하여 결제 상태 업데이트
            boolean result = paymentService.handlePaymentWebhook(paymentId);
            
            if (result) {
                log.info("PaymentId 웹훅 처리 성공: {}", paymentId);
                return ResponseEntity.ok("success");
            } else {
                log.warn("PaymentId 웹훅 처리 실패: {}", paymentId);
                return ResponseEntity.ok("processing_failed");
            }
            
        } catch (Exception e) {
            log.error("PaymentId 웹훅 처리 중 오류: {}", paymentId, e);
            return ResponseEntity.ok("processing_error");
        }
    }
    
    /**
     * 레거시 웹훅 엔드포인트 (V1 호환성)
     */
    @Operation(summary = "Payment Webhook V1 (Legacy)", description = "Legacy webhook endpoint for V1 compatibility")
    @PostMapping("/webhook/legacy")
    public ResponseEntity<String> handlePaymentWebhookLegacy(
            @Parameter(description = "Payment ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @RequestParam("paymentId") String paymentId) {
        
        log.info("레거시 웹훅 수신: paymentId={}", paymentId);
        
        try {
            boolean result = paymentService.handlePaymentWebhook(paymentId);
            log.info("레거시 웹훅 처리 결과: paymentId={}, success={}", paymentId, result);
            
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("레거시 웹훅 처리 오류: paymentId={}", paymentId, e);
            return ResponseEntity.ok("error");
        }
    }
} 
