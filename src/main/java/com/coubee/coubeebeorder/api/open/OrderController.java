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
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import com.coubee.coubeebeorder.domain.dto.BestsellerProductResponseDto;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
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

@Tag(name = "주문 API", description = "주문 생성, 조회, 취소를 위한 API")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Timed
@Counted
public class OrderController {

    private final OrderService orderService;
    private final StoreSecurityService storeSecurityService;
    private final UserServiceClient userServiceClient;

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성하고 결제를 준비합니다")
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    // 주문 생성 메트릭 수집을 위한 어노테이션
    @Timed(value = "order.creation.time", description = "주문 생성 소요 시간")
    @Counted(value = "order.creation.count", description = "생성된 주문 수")
    public ApiResponseDto<OrderCreateResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        OrderCreateResponse response = orderService.createOrder(userId, request);
        return ApiResponseDto.createOk(response);
    }

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다")
    @GetMapping("/orders/{orderId}")
    public ApiResponseDto<OrderDetailResponse> getOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderDetailResponse response = orderService.getOrder(orderId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "주문 상태 조회", description = "주문 ID로 현재 주문 상태를 조회합니다")
    @GetMapping("/orders/status/{orderId}")
    public ApiResponseDto<OrderStatusResponse> getOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderStatusResponse response = orderService.getOrderStatus(orderId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "내 주문 목록 조회", description = "인증된 사용자의 상세 주문 목록을 조회합니다. 키워드로 필터링 가능합니다.")
    @GetMapping("/users/me/orders")
    public ApiResponseDto<Page<OrderDetailResponse>> getMyOrders(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "상품명 검색 키워드", example = "베이컨")
            @RequestParam(required = false) String keyword) {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<OrderDetailResponse> response = orderService.getUserOrders(userId, pageRequest, keyword);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "내 주문 요약 조회", description = "인증된 사용자의 주문 요약 정보(총액, 건수)를 조회합니다.")
    @GetMapping("/users/me/summary")
    public ApiResponseDto<UserOrderSummaryDto> getMyOrderSummary() {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        UserOrderSummaryDto response = orderService.getUserOrderSummary(userId);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "주문 취소", description = "주문을 취소하고 결제를 환불합니다")
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

    @Operation(summary = "주문 수령 확인", description = "고객이 주문을 수령했음을 표시합니다")
    @PostMapping("/orders/{orderId}/receive")
    public ApiResponseDto<OrderDetailResponse> receiveOrder(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId) {
        OrderDetailResponse response = orderService.receiveOrder(orderId);
        return ApiResponseDto.createOk(response);
    }

    @Operation(summary = "주문 상태 변경", description = "주문 상태를 변경합니다 (매장 소유자만 가능)")
    @PatchMapping("/orders/{orderId}")
    public ApiResponseDto<OrderStatusUpdateResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "order_01H1J5BFXCZDMG8RP0WCTFSN5Y")
            @PathVariable String orderId,
            @Parameter(description = "User role from authentication", hidden = true)
            @RequestHeader("X-Auth-Role") String userRole,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        // 컨트롤러가 직접 헤더에서 userId를 가져옵니다.
        Long userId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        // 사용자가 주문 상태 변경 권한이 있는지 확인
        if (!"ROLE_ADMIN".equals(userRole) && !"ROLE_SUPER_ADMIN".equals(userRole)) {
            throw new IllegalArgumentException("관리자와 최고 관리자만 주문 상태를 변경할 수 있습니다");
        }

        OrderStatusUpdateResponse response = orderService.updateOrderStatus(orderId, request, userId);
        return ApiResponseDto.updateOk(response, "Order status has been updated");
    }

    @Operation(summary = "매장 주문 요약 조회", description = "매장 소유자를 위한 주문 요약 통계와 페이지네이션된 주문 목록을 조회합니다")
    @GetMapping("/stores/{storeId}/orders/summary")
    public ApiResponseDto<StoreOrderSummaryResponseDto> getStoreOrderSummary(
            @Parameter(description = "매장 ID", required = true, example = "1")
            @PathVariable Long storeId,
            @Parameter(description = "요약 기간 시작일", example = "2023-06-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "요약 기간 종료일", example = "2023-06-30")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        // X-Auth-UserId 헤더에서 소유자 사용자 ID 가져오기
        Long ownerUserId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        PageRequest pageRequest = PageRequest.of(page, size);
        StoreOrderSummaryResponseDto response = orderService.getStoreOrderSummary(
                ownerUserId, storeId, startDate, endDate, pageRequest);
        
        return ApiResponseDto.readOk(response);
    }

    // 매장 소유자가 자신의 주문을 조회하기 위한 새로운 엔드포인트
    @Operation(summary = "매장 주문 목록 조회", description = "특정 매장의 페이지네이션된 주문 목록을 조회합니다. 상태와 키워드로 필터링 가능하며, 최신순으로 정렬됩니다. (매장 소유자만 가능)")
    @GetMapping("/stores/{storeId}/orders")
    public ApiResponseDto<Page<OrderDetailResponse>> getStoreOrders(
            @Parameter(description = "인증에서 가져온 사용자 역할", hidden = true)
            @PathVariable Long storeId,
            @Parameter(description = "상태별 주문 필터링 (예: PAID, PREPARING, RECEIVED)")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "매장명, 상품명, 상품 설명 검색 키워드")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        // 헤더에서 소유자 ID 가져오기
        Long ownerUserId = GatewayRequestHeaderUtils.getUserIdOrThrowException();
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<OrderDetailResponse> response = orderService.getStoreOrders(ownerUserId, storeId, status, keyword, pageRequest);
        return ApiResponseDto.readOk(response);
    }

    @Operation(summary = "매장 주문 상세 조회", description = "인증된 매장 소유자를 위한 특정 주문의 상세 정보를 조회합니다")
    @GetMapping("/stores/{storeId}/orders/{orderId}")
    public ApiResponseDto<OrderDetailResponseDto> getStoreOrderDetails(
            @Parameter(description = "매장 ID", required = true, example = "1037")
            @PathVariable Long storeId,
            @Parameter(description = "주문 ID", required = true, example = "order_4aa75a911e78481c8195290208db1fc0")
            @PathVariable String orderId) {

        // 현재 인증된 사용자의 ID를 헤더에서 가져옵니다.
        Long authenticatedUserId = GatewayRequestHeaderUtils.getUserIdOrThrowException();

        // 요청을 보낸 사용자가 상점의 소유자인지 확인합니다.
        storeSecurityService.validateStoreOwner(authenticatedUserId, storeId);

        // 주문 ID를 사용하여 주문을 조회합니다.
        OrderDetailResponse orderDetail = orderService.getOrder(orderId);

        // 조회된 주문이 해당 상점에 속하는지 확인합니다.
        if (!orderDetail.getStoreId().equals(storeId)) {
            // 주문이 해당 상점에 속하지 않으면 404 에러를 발생시킵니다.
            throw new IllegalArgumentException("Order not found for this store");
        }

        // OpenFeign 클라이언트를 사용하여 사용자 정보를 조회합니다.
        ApiResponseDto<SiteUserInfoDto> userResponse = userServiceClient.getUserInfoById(orderDetail.getUserId());

        // User 서비스로부터 받은 SiteUserInfoDto를 직접 사용하여 DTO를 변환합니다.
        OrderDetailResponseDto responseDto = convertToOrderDetailResponseDto(orderDetail, userResponse.getData());

        return ApiResponseDto.<OrderDetailResponseDto>builder()
                .code("OK")
                .message("Order details retrieved successfully.")
                .data(responseDto)
                .build();
    }

    /**
     * OrderDetailResponse를 OrderDetailResponseDto로 변환하는 헬퍼 메서드
     */
    private OrderDetailResponseDto convertToOrderDetailResponseDto(OrderDetailResponse orderDetail, SiteUserInfoDto customerInfo) {
        // 상점 정보를 변환합니다.
        OrderDetailResponseDto.StoreInfo storeInfo = OrderDetailResponseDto.StoreInfo.builder()
                .storeId(orderDetail.getStore().getStoreId())
                .storeName(orderDetail.getStore().getStoreName())
                .build();

        // 주문 상품 목록을 변환합니다.
        List<OrderDetailResponseDto.OrderItemInfo> items = orderDetail.getItems().stream()
                .map(item -> OrderDetailResponseDto.OrderItemInfo.builder()
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        // 결제 정보를 변환합니다.
        OrderDetailResponseDto.PaymentInfo paymentInfo = null;
        if (orderDetail.getPayment() != null) {
            paymentInfo = OrderDetailResponseDto.PaymentInfo.builder()
                    .method(orderDetail.getPayment().getMethod())
                    .status(orderDetail.getPayment().getStatus())
                    .paidAt(orderDetail.getPayment().getPaidAt())
                    .build();
        }

        // 상태 변경 이력을 변환합니다.
        List<OrderDetailResponseDto.OrderStatusTimestampInfo> statusHistoryInfo = null;
        if (orderDetail.getStatusHistory() != null) {
            statusHistoryInfo = orderDetail.getStatusHistory().stream()
                    .map(history -> OrderDetailResponseDto.OrderStatusTimestampInfo.builder()
                            .status(history.getStatus().name())
                            .updatedAt(history.getUpdatedAt())
                            .build())
                    .toList();
        }

        // 최종 응답 DTO를 구성합니다.
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
                .statusHistory(statusHistoryInfo)
                .build();
    }

    @Operation(summary = "주변 베스트셀러 조회", description = "지정된 좌표 근처 매장의 베스트셀러 상품 목록을 페이지네이션으로 조회합니다")
    @GetMapping("/products/bestsellers-nearby")
    public ApiResponseDto<Page<BestsellerProductResponseDto>> getNearbyBestsellers(
            @Parameter(description = "위도 좌표", required = true, example = "37.5665")
            @RequestParam double latitude,
            @Parameter(description = "경도 좌표", required = true, example = "126.9780")
            @RequestParam double longitude,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<BestsellerProductResponseDto> response = orderService.getNearbyBestsellers(latitude, longitude, pageRequest);
        return ApiResponseDto.readOk(response);
    }

}