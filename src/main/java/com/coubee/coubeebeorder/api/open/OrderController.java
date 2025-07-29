package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.domain.dto.OrderCancelRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateResponse;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderListResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateResponse;
import com.coubee.coubeebeorder.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order API", description = "APIs for creating, retrieving, and cancelling orders")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create Order", description = "Creates a new order and prepares payment")
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<OrderCreateResponse> createOrder(
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-Auth-UserId") Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderCreateResponse response = orderService.createOrder(userId, request);
        return ApiResponseDto.createOk(response);
    }

    @Operation(summary = "Get Order Details", description = "Retrieves order details by order ID")
    @GetMapping("/orders/{orderId}")
    public ApiResponseDto<OrderDetailResponse> getOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderDetailResponse response = orderService.getOrder(orderId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get User Orders", description = "Retrieves order list by user ID")
    @GetMapping("/users/{userId}/orders")
    public ApiResponseDto<OrderListResponse> getUserOrders(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        OrderListResponse response = orderService.getUserOrders(userId, pageRequest);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Cancel Order", description = "Cancels an order and refunds payment")
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponseDto<OrderDetailResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId,
            @Valid @RequestBody OrderCancelRequest request) {
        OrderDetailResponse response = orderService.cancelOrder(orderId, request);
        return ApiResponseDto.createOk(response);
    }

    @Operation(summary = "Receive Order", description = "Marks an order as received by the customer")
    @PostMapping("/orders/{orderId}/receive")
    public ApiResponseDto<OrderDetailResponse> receiveOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderDetailResponse response = orderService.receiveOrder(orderId);
        return ApiResponseDto.createOk(response);
    }

    @Operation(summary = "Update Order Status", description = "Manually updates order status (Admin only)")
    @PatchMapping("/orders/{orderId}")
    public ApiResponseDto<OrderStatusUpdateResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderStatusUpdateResponse response = orderService.updateOrderStatus(orderId, request);
        return ApiResponseDto.createOk(response);
    }
}