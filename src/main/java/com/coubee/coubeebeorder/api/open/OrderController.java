package com.coubee.coubeebeorder.api.open;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.web.context.GatewayRequestHeaderUtils;
import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.dto.OrderCancelRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateResponse;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderListResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateResponse;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponseDto;
import com.coubee.coubeebeorder.domain.dto.StoreOrderSummaryResponseDto;
import com.coubee.coubeebeorder.domain.dto.UserOrderSummaryDto;
import com.coubee.coubeebeorder.service.OrderService;
import com.coubee.coubeebeorder.service.StoreSecurityService;
import com.coubee.coubeebeorder.remote.user.UserServiceClient;
import com.coubee.coubeebeorder.remote.user.SiteUserInfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Order API", description = "APIs for creating, retrieving, and cancelling orders")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final StoreSecurityService storeSecurityService;
    private final UserServiceClient userServiceClient;

    @Operation(summary = "Create Order", description = "Creates a new order and prepares payment")
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<OrderCreateResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

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

    @Operation(summary = "Get Order Status", description = "Retrieves the current status of an order by order ID")
    @GetMapping("/orders/status/{orderId}")
    public ApiResponseDto<OrderStatusResponse> getOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderStatusResponse response = orderService.getOrderStatus(orderId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get My Orders", description = "Retrieves detailed order list for authenticated user. Can be filtered by keyword.")
    @GetMapping("/users/me/orders")
    public ApiResponseDto<Page<OrderDetailResponse>> getMyOrders(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search keyword for product name", example = "bacon")
            @RequestParam(required = false) String keyword) {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<OrderDetailResponse> response = orderService.getUserOrders(userId, pageRequest, keyword);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get My Order Summary", description = "Retrieves order summary (total amounts, counts) for the authenticated user.")
    @GetMapping("/users/me/summary")
    public ApiResponseDto<UserOrderSummaryDto> getMyOrderSummary() {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        UserOrderSummaryDto response = orderService.getUserOrderSummary(userId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Cancel Order", description = "Cancels an order and refunds payment")
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponseDto<OrderDetailResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId,
            @Parameter(description = "User role from authentication", hidden = true)
            @RequestHeader("X-Auth-Role") String userRole,
            @Valid @RequestBody(required = false) OrderCancelRequest request) {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        OrderDetailResponse response = orderService.cancelOrder(orderId, request, userId, userRole);
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

    @Operation(summary = "Update Order Status", description = "Updates order status (Store owners only)")
    @PatchMapping("/orders/{orderId}")
    public ApiResponseDto<OrderStatusUpdateResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId,
            @Parameter(description = "User role from authentication", hidden = true)
            @RequestHeader("X-Auth-Role") String userRole,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        // Validate user has permission to update order status
        if (!"ROLE_ADMIN".equals(userRole) && !"ROLE_SUPER_ADMIN".equals(userRole)) {
            throw new IllegalArgumentException("Only admins and super admins can update order status");
        }

        OrderStatusUpdateResponse response = orderService.updateOrderStatus(orderId, request, userId);
        return ApiResponseDto.updateOk(response, "Order status has been updated");
    }

    @Operation(summary = "Get Store Order Summary", description = "Retrieves order summary statistics and paginated order list for store owners")
    @GetMapping("/stores/{storeId}/orders/summary")
    public ApiResponseDto<StoreOrderSummaryResponseDto> getStoreOrderSummary(
            @Parameter(description = "Store ID", required = true, example = "1")
            @PathVariable Long storeId,
            @Parameter(description = "Start date for summary period", example = "2023-06-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date for summary period", example = "2023-06-30")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        // Retrieve owner user ID from X-Auth-UserId header
        Long ownerUserId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        PageRequest pageRequest = PageRequest.of(page, size);
        StoreOrderSummaryResponseDto response = orderService.getStoreOrderSummary(
                ownerUserId, storeId, startDate, endDate, pageRequest);
        
        return ApiResponseDto.readOk(response);
    }

    // [ADD] New endpoint for store owners to view their orders.
    @Operation(summary = "Get Store Orders", description = "Retrieves a paginated list of orders for a specific store, optionally filtered by status and sorted by oldest first. (Store Owner only)")
    @GetMapping("/stores/{storeId}/orders")
    public ApiResponseDto<Page<OrderDetailResponse>> getStoreOrders(
            @Parameter(description = "ID of the store to retrieve orders for", required = true)
            @PathVariable Long storeId,
            @Parameter(description = "Filter orders by status (e.g., PAID, PREPARING, RECEIVED)")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        // (translation: Get owner ID from header.)
        Long ownerUserId = GatewayRequestHeaderUtils.getUserIdOrThrowException();
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<OrderDetailResponse> response = orderService.getStoreOrders(ownerUserId, storeId, status, pageRequest);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "Get Store Order Details", description = "Retrieves detailed information of a specific order for authenticated store owner")
    @GetMapping("/stores/{storeId}/orders/{orderId}")
    public ApiResponseDto<OrderDetailResponseDto> getStoreOrderDetails(
            @Parameter(description = "Store ID", required = true, example = "1037")
            @PathVariable Long storeId,
            @Parameter(description = "Order ID", required = true, example = "order_4aa75a911e78481c8195290208db1fc0")
            @PathVariable String orderId) {

        // 현재 인증된 사용자의 ID를 헤더에서 가져옵니다.
        // (Retrieves the currently authenticated user's ID from the header.)
        Long authenticatedUserId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        // 요청을 보낸 사용자가 상점의 소유자인지 확인합니다.
        // (Validates if the requesting user is the owner of the store.)
        storeSecurityService.validateStoreOwner(authenticatedUserId, storeId);

        // 주문 ID를 사용하여 주문을 조회합니다.
        // (Fetches the order using the order ID.)
        OrderDetailResponse orderDetail = orderService.getOrder(orderId);

        // 조회된 주문이 해당 상점에 속하는지 확인합니다.
        // (Verifies that the fetched order belongs to the specified store.)
        if (!orderDetail.getStoreId().equals(storeId)) {
            // 주문이 해당 상점에 속하지 않으면 404 에러를 발생시킵니다.
            // (If the order does not belong to the store, treat it as not found.)
            throw new IllegalArgumentException("Order not found for this store");
        }

        // OpenFeign 클라이언트를 사용하여 사용자 정보를 조회합니다.
        // (Fetch the user information using the OpenFeign client.)
        ApiResponseDto<SiteUserInfoDto> userResponse = userServiceClient.getUserInfoById(orderDetail.getUserId());

        // User 서비스로부터 받은 SiteUserInfoDto를 직접 사용하여 DTO를 변환합니다.
        // (Directly use the SiteUserInfoDto received from the User service to convert the DTO.)
        OrderDetailResponseDto responseDto = convertToOrderDetailResponseDto(orderDetail, userResponse.getData());

        return ApiResponseDto.<OrderDetailResponseDto>builder()
                .code("OK")
                .message("Order details retrieved successfully.")
                .data(responseDto)
                .success(true)
                .build();
    }

    /**
     * OrderDetailResponse를 OrderDetailResponseDto로 변환하는 헬퍼 메서드
     * (Helper method to convert OrderDetailResponse to OrderDetailResponseDto)
     */
    private OrderDetailResponseDto convertToOrderDetailResponseDto(OrderDetailResponse orderDetail, SiteUserInfoDto customerInfo) {
        // 상점 정보를 변환합니다.
        // (Converts store information.)
        OrderDetailResponseDto.StoreInfo storeInfo = OrderDetailResponseDto.StoreInfo.builder()
                .storeId(orderDetail.getStore().getStoreId())
                .storeName(orderDetail.getStore().getStoreName())
                .build();

        // 주문 상품 목록을 변환합니다.
        // (Converts order items list.)
        List<OrderDetailResponseDto.OrderItemInfo> items = orderDetail.getItems().stream()
                .map(item -> OrderDetailResponseDto.OrderItemInfo.builder()
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        // 결제 정보를 변환합니다.
        // (Converts payment information.)
        OrderDetailResponseDto.PaymentInfo paymentInfo = null;
        if (orderDetail.getPayment() != null) {
            paymentInfo = OrderDetailResponseDto.PaymentInfo.builder()
                    .method(orderDetail.getPayment().getMethod())
                    .status(orderDetail.getPayment().getStatus())
                    .paidAt(orderDetail.getPayment().getPaidAt())
                    .build();
        }

        // 최종 응답 DTO를 구성합니다.
        // (Builds the final response DTO.)
        return OrderDetailResponseDto.builder()
                .orderId(orderDetail.getOrderId())
                .status(orderDetail.getStatus().name())
                .totalAmount(orderDetail.getTotalAmount())
                .recipientName(orderDetail.getRecipientName())
                .createdAt(orderDetail.getCreatedAt())
                .store(storeInfo)
                .items(items)
                .payment(paymentInfo)
                .customerInfo(customerInfo)
                .build();
    }

}