package com.coubee.coubeebeorder.api.backend;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.domain.dto.UserOrderSummaryDto;
import com.coubee.coubeebeorder.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "백엔드 주문 API", description = "서비스 간 내부 통신을 위한 API")
@RestController
@RequestMapping("/backend/order")
@RequiredArgsConstructor
public class BackendOrderController {

    private final OrderService orderService;

    @Operation(summary = "사용자 주문 요약 조회 (백엔드용)", description = "특정 사용자 ID의 주문 요약 정보를 조회합니다")
    @GetMapping("/users/{userId}/summary")
    public ApiResponseDto<UserOrderSummaryDto> getUserOrderSummary(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        UserOrderSummaryDto response = orderService.getUserOrderSummary(userId);
        return ApiResponseDto.readOk(response);
    }
}
