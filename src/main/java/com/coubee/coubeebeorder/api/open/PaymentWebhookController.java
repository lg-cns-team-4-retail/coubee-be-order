package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.service.PaymentService;
import com.coubee.coubeebeorder.util.PortOneWebhookVerifier;
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

    @PostMapping("/portone")
    @Operation(summary = "PortOne 웹훅", description = "PortOne에서 전송하는 결제 완료 웹훅을 처리합니다.")
    public ResponseEntity<ApiResponseDto<String>> handlePortOneWebhook(
            @RequestBody String requestBody,
            @Parameter(description = "웹훅 서명")
            @RequestHeader(value = "X-IamPort-Signature", required = false) String signature,
            @Parameter(description = "웹훅 타임스탬프")  
            @RequestHeader(value = "X-IamPort-Timestamp", required = false) String timestamp,
            @Parameter(description = "결제 ID")
            @RequestParam(value = "imp_uid", required = false) String paymentId) {
        
        log.info("PortOne 웹훅 수신 - 결제 ID: {}", paymentId);
        log.debug("웹훅 요청 본문: {}", requestBody);
        
        try {
            if (webhookVerifier.isWebhookSecretConfigured()) {
                boolean isValidSignature = webhookVerifier.verifyWebhook(requestBody, signature, timestamp);
                if (!isValidSignature) {
                    log.warn("웹훅 서명 검증 실패 - 결제 ID: {}", paymentId);
                    return ResponseEntity.badRequest()
                            .body(ApiResponseDto.createError("WEBHOOK_VERIFICATION_FAILED", "웹훅 서명이 유효하지 않습니다."));
                }
                log.info("웹훅 서명 검증 성공 - 결제 ID: {}", paymentId);
            } else {
                log.warn("웹훅 시크릿이 설정되지 않아 서명 검증을 건너뜁니다.");
            }
            
            if (paymentId == null || paymentId.trim().isEmpty()) {
                log.warn("결제 ID가 누락되었습니다.");
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.createError("PAYMENT_ID_REQUIRED", "결제 ID가 필요합니다."));
            }
            
            boolean processed = paymentService.handlePaymentWebhook(paymentId);
            
            if (processed) {
                log.info("웹훅 처리 완료 - 결제 ID: {}", paymentId);
                return ResponseEntity.ok(ApiResponseDto.createOk("웹훅 처리 완료"));
            } else {
                log.warn("웹훅 처리 실패 - 결제 ID: {}", paymentId);
                return ResponseEntity.internalServerError()
                        .body(ApiResponseDto.createError("WEBHOOK_PROCESSING_FAILED", "웹훅 처리 중 오류가 발생했습니다."));
            }
            
        } catch (Exception e) {
            log.error("웹훅 처리 중 예외 발생 - 결제 ID: " + paymentId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.createError("INTERNAL_SERVER_ERROR", "웹훅 처리 중 내부 오류가 발생했습니다."));
        }
    }
}