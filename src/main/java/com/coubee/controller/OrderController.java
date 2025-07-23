package com.coubee.controller;

import com.coubee.dto.request.OrderCancelRequest;
import com.coubee.dto.request.OrderCreateRequest;
import com.coubee.dto.request.OrderStatusUpdateRequest;
import com.coubee.dto.response.ApiResponse;
import com.coubee.dto.response.OrderCreateResponse;
import com.coubee.dto.response.OrderDetailResponse;
import com.coubee.dto.response.OrderListResponse;
import com.coubee.dto.response.OrderStatusUpdateResponse;
import com.coubee.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Order API Controller
 */
@Tag(name = "Order API", description = "APIs for creating, retrieving, and cancelling orders")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create Order API
     *
     * @param userId  User ID
     * @param request Order creation request
     * @return Order creation response
     */
    @Operation(summary = "Create Order", description = "Creates a new order and prepares payment")
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderCreateResponse> createOrder(
            @Parameter(description = "User ID", required = true)
            @RequestHeader("X-User-ID") Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderCreateResponse response = orderService.createOrder(userId, request);
        return ApiResponse.success(HttpStatus.CREATED, "Order has been created", response);
    }

    /**
     * Get Order Details API
     *
     * @param orderId Order ID
     * @return Order detail information
     */
    @Operation(summary = "Get Order Details", description = "Retrieves order details by order ID")
    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderDetailResponse response = orderService.getOrder(orderId);
        return ApiResponse.success(response);
    }

    /**
     * Get User Orders API
     *
     * @param userId User ID
     * @param page   Page number
     * @param size   Page size
     * @return Order list
     */
    @Operation(summary = "Get User Orders", description = "Retrieves order list by user ID")
    @GetMapping("/users/{userId}/orders")
    public ApiResponse<OrderListResponse> getUserOrders(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        OrderListResponse response = orderService.getUserOrders(userId, pageRequest);
        return ApiResponse.success(response);
    }

    /**
     * Cancel Order API
     *
     * @param orderId Order ID
     * @param request Order cancellation request
     * @return Cancelled order details
     */
    @Operation(summary = "Cancel Order", description = "Cancels an order and refunds payment")
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<OrderDetailResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId,
            @Valid @RequestBody OrderCancelRequest request) {
        OrderDetailResponse response = orderService.cancelOrder(orderId, request);
        return ApiResponse.success("Order has been cancelled", response);
    }

    /**
     * Receive Order API
     *
     * @param orderId Order ID
     * @return Received order details
     */
    @Operation(summary = "Receive Order", description = "Marks an order as received by the customer")
    @PostMapping("/orders/{orderId}/receive")
    public ApiResponse<OrderDetailResponse> receiveOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderDetailResponse response = orderService.receiveOrder(orderId);
        return ApiResponse.success("Order has been received", response);
    }

    /**
     * Update Order Status API (Manual)
     *
     * @param orderId Order ID
     * @param request Order status update request
     * @return Updated order status information
     */
    @Operation(summary = "Update Order Status", description = "Manually updates order status (Admin only)")
    @PatchMapping("/orders/{orderId}")
    public ApiResponse<OrderStatusUpdateResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderStatusUpdateResponse response = orderService.updateOrderStatus(orderId, request);
        return ApiResponse.success("Order status has been updated", response);
    }
} 
