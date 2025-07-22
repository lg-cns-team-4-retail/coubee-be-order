package com.coubee.controller;

import com.coubee.service.OrderService;
import com.coubee.service.QrCodeService;
import com.coubee.dto.response.OrderDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * QR Code Controller for order-related QR code operations
 */
@Slf4j
@Tag(name = "QR Code API", description = "QR code generation and lookup APIs")
@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final OrderService orderService;

    /**
     * Generate QR code image for order ID
     */
    @Operation(
        summary = "Generate QR Code for Order", 
        description = "Generate QR code image containing the order ID"
    )
    @GetMapping("/orders/{orderId}/image")
    public ResponseEntity<byte[]> generateOrderQrCode(
            @Parameter(description = "Order ID", example = "order_b7833686f25b48e0862612345678abcd")
            @PathVariable String orderId) {
        
        try {
            log.info("Generating QR code for order: {}", orderId);
            
            // Generate QR code image
            byte[] qrCodeImage = qrCodeService.generateOrderIdQrCode(orderId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeImage.length);
            headers.set("Content-Disposition", "inline; filename=\"order-" + orderId + "-qr.png\"");
            
            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Failed to generate QR code for order: {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Look up order by QR token
     */
    @Operation(
        summary = "Lookup Order by QR Token", 
        description = "Find order information using QR token (Base64 encoded order ID)"
    )
    @GetMapping("/lookup/{qrToken}")
    public ResponseEntity<OrderDetailResponse> lookupOrderByQrToken(
            @Parameter(description = "QR Token (Base64 encoded)", example = "b3JkZXJfYjc4MzM2ODZmMjViNDhlMDg2MjYxMjM0NTY3OGFiY2Q")
            @PathVariable String qrToken) {
        
        try {
            log.info("Looking up order by QR token: {}", qrToken);
            
            // Decode QR token to get order ID
            String orderId = new String(Base64.getUrlDecoder().decode(qrToken));
            log.info("Decoded order ID: {}", orderId);
            
            // Get order details
            OrderDetailResponse orderDetail = orderService.getOrder(orderId);
            
            return ResponseEntity.ok(orderDetail);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid QR token format: {}", qrToken, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to lookup order by QR token: {}", qrToken, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate QR code with lookup URL
     */
    @Operation(
        summary = "Generate QR Code with Lookup URL", 
        description = "Generate QR code containing a URL to lookup the order"
    )
    @GetMapping("/orders/{orderId}/url")
    public ResponseEntity<byte[]> generateOrderQrCodeWithUrl(
            @Parameter(description = "Order ID", example = "order_b7833686f25b48e0862612345678abcd")
            @PathVariable String orderId,
            @Parameter(description = "Base URL for lookup", example = "https://api.example.com")
            @RequestParam(defaultValue = "http://localhost:8083") String baseUrl) {
        
        try {
            log.info("Generating QR code with URL for order: {}", orderId);
            
            // Generate QR code with lookup URL
            String lookupUrl = baseUrl + "/api/orders/" + orderId;
            byte[] qrCodeImage = qrCodeService.generateQrCodeImage(lookupUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeImage.length);
            headers.set("Content-Disposition", "inline; filename=\"order-" + orderId + "-url-qr.png\"");
            
            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Failed to generate QR code with URL for order: {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}