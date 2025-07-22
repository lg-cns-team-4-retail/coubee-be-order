package com.coubee.client;

import com.coubee.client.dto.request.PortOnePaymentCancelRequest;
import com.coubee.client.dto.request.PortOnePaymentRequest;
import com.coubee.client.dto.response.PortOnePaymentCancelResponse;
import com.coubee.client.dto.response.PortOnePaymentResponse;
import com.coubee.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * PortOne API Client (V1 + V2 지원)
 */
@FeignClient(
    name = "portone", 
    url = "${portone.api.url}", 
    configuration = FeignConfig.class
)
public interface PortOneClient {

    // ========== V1 API (기존 호환성) ==========
    
    /**
     * V1: Prepare payment (기존 호환성)
     */
    @PostMapping("/payments/prepare")
    PortOnePaymentResponse preparePayment(@RequestBody PortOnePaymentRequest request);

    /**
     * V1: Get payment information (기존 호환성)
     */
    @GetMapping("/payments/{paymentId}")
    PortOnePaymentResponse getPayment(@PathVariable("paymentId") String paymentId);

    /**
     * V1: Cancel payment (기존 호환성)
     */
    @PostMapping("/payments/{paymentId}/cancel")
    PortOnePaymentCancelResponse cancelPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody PortOnePaymentCancelRequest request);

    // ========== V2 API (신규) ==========
    
    /**
     * V2: Create payment
     */
    @PostMapping("/v2/payments")
    PortOnePaymentResponse createPaymentV2(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("storeId") String storeId,
            @RequestBody PortOnePaymentRequest request);

    /**
     * V2: Get payment information
     */
    @GetMapping("/v2/payments/{paymentId}")
    PortOnePaymentResponse getPaymentV2(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("paymentId") String paymentId);

    /**
     * V2: Cancel payment
     */
    @PostMapping("/v2/payments/{paymentId}/cancel")
    PortOnePaymentCancelResponse cancelPaymentV2(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("paymentId") String paymentId,
            @RequestBody PortOnePaymentCancelRequest request);
            
    /**
     * V2: Get access token
     */
    @PostMapping("/v2/login/api-secret")
    Object getAccessToken(@RequestBody Object request);
} 
