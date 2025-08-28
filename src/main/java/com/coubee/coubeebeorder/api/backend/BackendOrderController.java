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

@Tag(name = "Backend Order API", description = "Internal APIs for inter-service communication")
@RestController
@RequestMapping("/backend/order")
@RequiredArgsConstructor
public class BackendOrderController {

    private final OrderService orderService;

    @Operation(summary = "Get User Order Summary (For Backend)", description = "Retrieves order summary for a specific user ID.")
    @GetMapping("/users/{userId}/summary")
    public ApiResponseDto<UserOrderSummaryDto> getUserOrderSummary(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        UserOrderSummaryDto response = orderService.getUserOrderSummary(userId);
        return ApiResponseDto.readOk(response);
    }
}
