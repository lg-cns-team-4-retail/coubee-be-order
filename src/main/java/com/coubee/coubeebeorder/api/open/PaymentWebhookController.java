package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.remote.dto.PortoneWebhookPayload;
import com.coubee.coubeebeorder.service.PaymentService;
import com.coubee.coubeebeorder.util.PortOneWebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/order/webhook")
@RequiredArgsConstructor
@Tag(name = "Payment Webhook", description = "결제 웹훅 API")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final PortOneWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/portone")
    @Operation(summary = "PortOne 웹훅", description = "PortOne에서 전송하는 결제 완료 웹훅을 처리합니다.")
    public ResponseEntity<ApiResponseDto<String>> handlePortOneWebhook(
            @RequestBody String requestBody,
            @Parameter(description = "웹훅 서명")
            @RequestHeader(value = "PortOne-Signature-v2", required = false) String signature, // V2 헤더 이름 확인 필요
            @Parameter(description = "웹훅 타임스탬프")  
            @RequestHeader(value = "PortOne-Request-Timestamp", required = false) String timestamp) { // V2 헤더 이름 확인 필요
        
        log.info("PortOne V2 웹훅 수신");
        log.info("PortOne Webhook Received - Raw Body: {}", requestBody);
        
        String transactionId = "unknown";
        
        try {
            PortoneWebhookPayload payload = objectMapper.readValue(requestBody, PortoneWebhookPayload.class);
            transactionId = payload.getTxId();
            
            log.info("웹훅 페이로드 파싱 완료 - 거래 ID(tx_id): {}, 상태: {}, 주문 ID(payment_id): {}", 
                    transactionId, payload.getStatus(), payload.getPaymentId());

            if (webhookVerifier.isWebhookSecretConfigured()) {
                if (!webhookVerifier.verifyWebhook(requestBody, signature, timestamp)) {
                    log.warn("웹훅 서명 검증 실패 - 거래 ID: {}", transactionId);
                    return ResponseEntity.badRequest()
                            .body(ApiResponseDto.createError("WEBHOOK_VERIFICATION_FAILED", "웹훅 서명이 유효하지 않습니다."));
                }
                log.info("웹훅 서명 검증 성공 - 거래 ID: {}", transactionId);
            } else {
                log.warn("웹훅 시크릿이 설정되지 않아 서명 검증을 건너뜁니다.");
            }
            
            if (transactionId == null || transactionId.trim().isEmpty()) {
                log.warn("거래 ID(tx_id)가 누락되었습니다.");
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.createError("TRANSACTION_ID_REQUIRED", "거래 ID가 필요합니다."));
            }
            
            // 서비스 로직에는 PortOne의 고유 거래 ID (tx_id)를 전달합니다.
            boolean processed = paymentService.handlePaymentWebhook(transactionId);
            
            if (processed) {
                log.info("웹훅 처리 완료 - 거래 ID: {}", transactionId);
                return ResponseEntity.ok(ApiResponseDto.createOk("웹훅 처리 완료"));
            } else {
                log.warn("웹훅 처리 실패 - 거래 ID: {}", transactionId);
                return ResponseEntity.internalServerError()
                        .body(ApiResponseDto.createError("WEBHOOK_PROCESSING_FAILED", "웹훅 처리 중 오류가 발생했습니다."));
            }
            
        } catch (Exception e) {
            log.error("웹훅 처리 중 예외 발생 - 거래 ID: " + transactionId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.createError("INTERNAL_SERVER_ERROR", "웹훅 처리 중 내부 오류가 발생했습니다."));
        }
    }
}