package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.config.PortOneProperties;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyRequest;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyResponse;
import com.coubee.coubeebeorder.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/order/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "결제 관련 API")
public class PaymentController {

    private final PaymentService paymentService;
    private final PortOneProperties portOneProperties;

    @PostMapping("/orders/{orderId}/prepare")
    @Operation(summary = "결제 준비", description = "주문에 대한 결제를 준비합니다.")
    public ResponseEntity<ApiResponseDto<PaymentReadyResponse>> preparePayment(
            @Parameter(description = "주문 ID", example = "order_b7833686f25b48e0862612345678abcd")
            @PathVariable String orderId,
            @Valid @RequestBody PaymentReadyRequest request) {
        
        log.info("결제 준비 요청 - 주문 ID: {}, 매장 ID: {}", orderId, request.storeId());
        
        PaymentReadyResponse response = paymentService.preparePayment(orderId, request);
        
        log.info("결제 준비 완료 - merchant_uid: {}", response.merchantUid());
        
        return ResponseEntity.ok(ApiResponseDto.createOk(response));
    }

    @GetMapping("/{paymentId}/status")
    @Operation(summary = "결제 상태 조회", description = "결제 ID로 결제 상태를 조회합니다.")
    public ResponseEntity<ApiResponseDto<Object>> getPaymentStatus(
            @Parameter(description = "결제 ID", example = "imp_123456789")
            @PathVariable String paymentId) {
        
        log.info("결제 상태 조회 요청 - 결제 ID: {}", paymentId);
        
        Object response = paymentService.getPaymentStatus(paymentId);
        
        return ResponseEntity.ok(ApiResponseDto.readOk(response));
    }

    @GetMapping("/config")
    @Operation(summary = "결제 설정 정보 조회", description = "PortOne 설정 정보를 제공합니다.")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getPaymentConfig() {
        
        log.info("결제 설정 정보 조회 요청");
        
        Map<String, Object> config = Map.of(
            "storeId", portOneProperties.getStoreId(),
            "channelKeys", portOneProperties.getChannels()
        );
        
        log.info("결제 설정 정보 응답 - Store ID: {}", portOneProperties.getStoreId());
        
        return ResponseEntity.ok(ApiResponseDto.readOk(config));
    }
}