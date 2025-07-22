package com.coubee.controller;

import com.coubee.config.PortOneProperties;
import com.coubee.dto.request.PaymentReadyRequest;
import com.coubee.dto.response.ApiResponse;
import com.coubee.dto.response.PaymentReadyResponse;
import com.coubee.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment API Controller for payment preparation and management
 */
@Tag(name = "Payment API", description = "APIs for payment preparation and management")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PortOneProperties portOneProperties;

    /**
     * Prepare Payment API
     * Prepares payment information for PortOne integration
     *
     * @param orderId Order ID
     * @param request Payment preparation request
     * @return Payment preparation response
     */
    @Operation(summary = "Prepare Payment", description = "Prepares payment information for order")
    @PostMapping("/prepare")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PaymentReadyResponse> preparePayment(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @RequestParam("orderId") String orderId,
            @Valid @RequestBody PaymentReadyRequest request) {
        
        PaymentReadyResponse response = paymentService.preparePayment(orderId, request);
        return ApiResponse.success("Payment preparation completed", response);
    }

    /**
     * Get Payment Status API
     * Retrieves current payment status by payment ID
     *
     * @param paymentId Payment ID
     * @return Payment status information
     */
    @Operation(summary = "Get Payment Status", description = "Retrieves payment status by payment ID")
    @GetMapping("/{paymentId}/status")
    public ApiResponse<Object> getPaymentStatus(
            @Parameter(description = "Payment ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String paymentId) {
        
        Object paymentStatus = paymentService.getPaymentStatus(paymentId);
        return ApiResponse.success(paymentStatus);
    }

    /**
     * Get Payment Configuration API
     * Provides PortOne configuration for frontend
     *
     * @return Payment configuration including store ID and channel keys
     */
    @Operation(summary = "Get Payment Configuration", description = "Provides PortOne configuration for frontend")
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getPaymentConfig() {
        
        Map<String, Object> config = Map.of(
            "storeId", portOneProperties.getStoreId(),
            "channelKeys", portOneProperties.getChannels()
        );
        
        return ApiResponse.success(config);
    }
}