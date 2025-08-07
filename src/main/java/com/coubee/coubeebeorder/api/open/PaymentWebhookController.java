package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.service.PaymentService;
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

    @PostMapping("/portone")
    @Operation(summary = "PortOne 웹훅", description = "PortOne에서 전송하는 결제 완료 웹훅을 처리합니다.")
    public ResponseEntity<ApiResponseDto<String>> handlePortOneWebhook(
            @RequestBody String requestBody,
            @Parameter(description = "웹훅 서명")
            @RequestHeader(value = "webhook-signature", required = false) String signature,
            @Parameter(description = "웹훅 타임스탬프")  
            @RequestHeader(value = "webhook-timestamp", required = false) String timestamp) {
        
        log.info("PortOne V2 Standard Webhook 수신. Service layer로 처리 위임.");
        log.debug("Webhook Raw Body: {}", requestBody);

        boolean processed = paymentService.handlePaymentWebhook(signature, timestamp, requestBody);

        if (processed) {
            log.info("Webhook processed successfully by service.");
            return ResponseEntity.ok(ApiResponseDto.createOk("웹훅 처리 완료"));
        } else {
            log.warn("Webhook processing failed by service.");
            // 서비스에서 false를 반환하면 클라이언트 오류(400) 또는 서버 오류(500)로 응답
            // 서명 실패 등은 400이 더 적절할 수 있음.
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.createError("WEBHOOK_PROCESSING_FAILED", "웹훅 처리 중 오류가 발생했습니다."));
        }
    }
}