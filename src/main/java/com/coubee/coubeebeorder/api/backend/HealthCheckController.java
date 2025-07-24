package com.coubee.coubeebeorder.api.backend;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
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

@Tag(name = "Health Check API", description = "APIs for service health monitoring")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    @Operation(summary = "Health Check", description = "Returns service health status")
    @GetMapping
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "coubee-be-order");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("version", "1.0.0");
        
        ApiResponseDto<Map<String, Object>> response = ApiResponseDto.readOk(healthInfo);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Readiness Check", description = "Returns service readiness status")
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readinessCheck() {
        Map<String, String> status = new HashMap<>();
        
        try {
            status.put("status", "ready");
            status.put("message", "Service is ready to handle requests");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("status", "not ready");
            status.put("message", "Service is not ready: " + e.getMessage());
            return ResponseEntity.status(503).body(status);
        }
    }

    @Operation(summary = "Liveness Check", description = "Returns service liveness status")
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> livenessCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "alive");
        status.put("message", "Service is alive");
        return ResponseEntity.ok(status);
    }
}