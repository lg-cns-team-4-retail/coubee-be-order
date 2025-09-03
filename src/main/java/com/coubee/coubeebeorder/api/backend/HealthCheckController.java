package com.coubee.coubeebeorder.api.backend;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.remote.store.StoreClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Health Check API", description = "APIs for service health monitoring")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthCheckController {
    private final StoreClient storeClient; // ★★★ StoreClient 주입
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

    // ★★★★★ 아래 테스트용 엔드포인트를 추가합니다. ★★★★★
    @Operation(summary = "[Test] Get Owner ID by Store ID", description = "Tests the Feign client call to get an owner ID from the store service.")
    @GetMapping("/test/store/{storeId}/owner")
    public ApiResponseDto<Long> testGetOwnerIdByStoreId(
            @Parameter(description = "Store ID to test", required = true, example = "1037")
            @PathVariable Long storeId) {
        
        log.info("[FEIGN-TEST] storeClient.getOwnerIdByStoreId({}) 호출을 시작합니다.", storeId);
        
        try {
            ApiResponseDto<Long> response = storeClient.getOwnerIdByStoreId(storeId);
            
            log.info("[FEIGN-TEST] 호출 성공. 응답: {}", response);
            if (response != null) {
                log.info("[FEIGN-TEST] 응답 코드: {}, 메시지: {}, 데이터: {}", response.getCode(), response.getMessage(), response.getData());
            }
            
            return response;

        } catch (Exception e) {
            log.error("[FEIGN-TEST] FeignClient 호출 중 예외 발생!", e);
            // 예외 발생 시, 에러 응답을 직접 생성하여 반환
            return ApiResponseDto.createError("FEIGN_CLIENT_ERROR", e.getMessage(), null);
        }
    }
}