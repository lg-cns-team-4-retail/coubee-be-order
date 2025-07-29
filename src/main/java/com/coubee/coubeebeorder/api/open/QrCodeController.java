package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/order/qr")
@RequiredArgsConstructor
@Tag(name = "QR Code", description = "QR 코드 생성 API")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @GetMapping(value = "/orders/{orderId}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "주문 QR 코드 생성", description = "주문 ID로 QR 코드 이미지를 생성합니다.")
    public ResponseEntity<byte[]> generateOrderQrCode(
            @Parameter(description = "주문 ID", example = "order_b7833686f25b48e0862612345678abcd")
            @PathVariable String orderId,
            @Parameter(description = "QR 코드 크기", example = "200")
            @RequestParam(defaultValue = "200") int size) {
        
        log.info("주문 QR 코드 생성 요청 - 주문 ID: {}, 크기: {}", orderId, size);
        
        byte[] qrCodeImage = qrCodeService.generateOrderIdQrCode(orderId);
        
        log.info("주문 QR 코드 생성 완료 - 주문 ID: {}", orderId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeImage);
    }

    @GetMapping(value = "/payment/{merchantUid}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "결제 QR 코드 생성", description = "merchant_uid로 결제용 QR 코드 이미지를 생성합니다.")
    public ResponseEntity<byte[]> generatePaymentQrCode(
            @Parameter(description = "Merchant UID", example = "order_b7833686f25b48e0862612345678abcd")
            @PathVariable String merchantUid,
            @Parameter(description = "QR 코드 크기", example = "200")
            @RequestParam(defaultValue = "200") int size) {
        
        log.info("결제 QR 코드 생성 요청 - Merchant UID: {}, 크기: {}", merchantUid, size);
        
        byte[] qrCodeImage = qrCodeService.generateQrCodeImage(merchantUid);
        
        log.info("결제 QR 코드 생성 완료 - Merchant UID: {}", merchantUid);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeImage);
    }
}