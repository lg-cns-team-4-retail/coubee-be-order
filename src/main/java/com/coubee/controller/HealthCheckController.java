package com.coubee.controller;

import com.coubee.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller for service monitoring
 */
@Tag(name = "Health Check API", description = "APIs for service health monitoring")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    /**
     * Basic health check endpoint
     *
     * @return Health status response
     */
    @Operation(summary = "Health Check", description = "Returns service health status")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "coubee-order-payment-service");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("version", "1.0.0");
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
            "Service is healthy", 
            healthInfo
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness probe endpoint
     *
     * @return Readiness status response
     */
    @Operation(summary = "Readiness Check", description = "Returns service readiness status")
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readinessCheck() {
        Map<String, String> status = new HashMap<>();
        
        try {
            // Add actual readiness checks here (database connectivity, external service availability, etc.)
            status.put("status", "ready");
            status.put("message", "Service is ready to handle requests");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("status", "not ready");
            status.put("message", "Service is not ready: " + e.getMessage());
            return ResponseEntity.status(503).body(status);
        }
    }

    /**
     * Liveness probe endpoint
     *
     * @return Liveness status response
     */
    @Operation(summary = "Liveness Check", description = "Returns service liveness status")
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> livenessCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "alive");
        status.put("message", "Service is alive");
        return ResponseEntity.ok(status);
    }
}