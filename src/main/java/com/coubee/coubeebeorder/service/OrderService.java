package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.OrderStatus;
import com.coubee.coubeebeorder.domain.dto.OrderCancelRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderCreateResponse;
import com.coubee.coubeebeorder.domain.dto.OrderDetailResponse;
import com.coubee.coubeebeorder.domain.dto.OrderListResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusResponse;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateRequest;
import com.coubee.coubeebeorder.domain.dto.OrderStatusUpdateResponse;
import com.coubee.coubeebeorder.domain.dto.StoreOrderSummaryResponseDto;
import com.coubee.coubeebeorder.domain.dto.UserOrderSummaryDto;
import com.coubee.coubeebeorder.domain.dto.BestsellerProductResponseDto;
import com.coubee.coubeebeorder.remote.product.ProductResponseDto;
import com.coubee.coubeebeorder.remote.user.SiteUserInfoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrderService {

    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request);

    OrderDetailResponse getOrder(String orderId);

    OrderStatusResponse getOrderStatus(String orderId);

    Page<OrderDetailResponse> getUserOrders(Long userId, Pageable pageable, String keyword);

    OrderDetailResponse cancelOrder(String orderId, OrderCancelRequest request, Long userId, String userRole);

    OrderDetailResponse receiveOrder(String orderId);

    OrderStatusUpdateResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request, Long userId);

    /**
     * 주문 상태를 변경하고 이력을 기록합니다 (내부 서비스용)
     *
     * @param orderId 주문 ID
     * @param newStatus 새로운 주문 상태
     */
    void updateOrderStatusWithHistory(String orderId, OrderStatus newStatus);

    /**
     * 사용자 주문 요약 정보를 조회합니다 (백엔드 서비스용)
     *
     * @param userId 사용자 ID
     * @return 집계된 통계를 포함한 사용자 주문 요약 정보
     */
    UserOrderSummaryDto getUserOrderSummary(Long userId);

    /**
     * 매장 주문 요약 정보와 페이지네이션된 주문 목록을 조회합니다
     *
     * @param ownerUserId 매장 소유자 사용자 ID
     * @param storeId 매장 ID
     * @param startDate 요약 기간 시작일 (선택사항)
     * @param endDate 요약 기간 종료일 (선택사항)
     * @param pageable 페이지네이션 정보
     * @return 통계와 상세 주문 목록을 포함한 매장 주문 요약 응답
     */
    StoreOrderSummaryResponseDto getStoreOrderSummary(Long ownerUserId, Long storeId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 매장의 주문 목록을 조회하기 위한 새로운 메소드
    Page<OrderDetailResponse> getStoreOrders(Long ownerUserId, Long storeId, OrderStatus status, String keyword, Pageable pageable);

    /**
     * 지리적 좌표를 기반으로 주변 베스트셀러 상품을 조회합니다
     *
     * @param latitude 위도 좌표
     * @param longitude 경도 좌표
     * @param pageable 페이지네이션 정보
     * @return 주변 매장의 베스트셀러 상품 페이지네이션 목록
     */
    Page<BestsellerProductResponseDto> getNearbyBestsellers(double latitude, double longitude, Pageable pageable);

    /**
     * Circuit Breaker가 적용된 개별 사용자 데이터 조회
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 (실패 시 폴백 데이터)
     */
    SiteUserInfoDto getUserData(Long userId);
}