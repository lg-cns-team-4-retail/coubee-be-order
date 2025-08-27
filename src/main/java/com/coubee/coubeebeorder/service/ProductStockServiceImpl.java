package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.common.exception.ApiError;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.remote.product.ProductClient;
import com.coubee.coubeebeorder.remote.product.StockUpdateRequest;
import com.coubee.coubeebeorder.remote.product.StockUpdateResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 재고 관리 서비스 구현체
 * Product Service와 동기적으로 통신하여 재고를 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final ProductClient productClient;

    @Override
    @CircuitBreaker(name = "productStock", fallbackMethod = "decreaseStockFallback")
    public void decreaseStock(Order order) {
        log.info("재고 감소 요청 - 주문 ID: {}, 매장 ID: {}", order.getOrderId(), order.getStoreId());

        try {
            // 주문 아이템들을 재고 감소 요청으로 변환 (음수로 설정)
            List<StockUpdateRequest.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockUpdateRequest.StockItem.builder()
                            .productId(item.getProductId())
                            .quantityChange(-item.getQuantity()) // 음수로 설정하여 재고 감소
                            .build())
                    .collect(Collectors.toList());

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .storeId(order.getStoreId())
                    .items(stockItems)
                    .build();

            // Product Service에 재고 감소 요청
            ApiResponseDto<StockUpdateResponse> response = productClient.updateStock(request, order.getUserId());

            if (response.isSuccess() && response.getData() != null && response.getData().getSuccess()) {
                log.info("재고 감소 성공 - 주문 ID: {}", order.getOrderId());
                logStockUpdateDetails(response.getData(), "감소");
            } else {
                log.error("재고 감소 실패 - 주문 ID: {}, 응답: {}", order.getOrderId(), response);
                throw new RuntimeException("재고 감소에 실패했습니다: " +
                    (response.getData() != null ? response.getData().getMessage() : "알 수 없는 오류"));
            }

        } catch (Exception e) {
            log.error("재고 감소 중 오류 발생 - 주문 ID: {}", order.getOrderId(), e);
            throw e; // InsufficientStockException이나 다른 예외를 그대로 전파
        }
    }

    @Override
    @CircuitBreaker(name = "productStock", fallbackMethod = "increaseStockFallback")
    public void increaseStock(Order order) {
        log.info("재고 증가 요청 - 주문 ID: {}, 매장 ID: {}", order.getOrderId(), order.getStoreId());

        try {
            // 주문 아이템들을 재고 증가 요청으로 변환 (양수로 설정)
            List<StockUpdateRequest.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockUpdateRequest.StockItem.builder()
                            .productId(item.getProductId())
                            .quantityChange(item.getQuantity()) // 양수로 설정하여 재고 증가
                            .build())
                    .collect(Collectors.toList());

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .storeId(order.getStoreId())
                    .items(stockItems)
                    .build();

            // Product Service에 재고 증가 요청
            ApiResponseDto<StockUpdateResponse> response = productClient.updateStock(request, order.getUserId());

            if (response.isSuccess() && response.getData() != null && response.getData().getSuccess()) {
                log.info("재고 증가 성공 - 주문 ID: {}", order.getOrderId());
                logStockUpdateDetails(response.getData(), "증가");
            } else {
                log.error("재고 증가 실패 - 주문 ID: {}, 응답: {}", order.getOrderId(), response);
                // 재고 증가는 보상 트랜잭션이므로 실패해도 예외를 던지지 않고 로그만 남김
                log.warn("재고 증가 실패는 보상 트랜잭션이므로 처리를 계속합니다.");
            }

        } catch (Exception e) {
            log.error("재고 증가 중 오류 발생 - 주문 ID: {}", order.getOrderId(), e);
            // 재고 증가는 보상 트랜잭션이므로 실패해도 예외를 던지지 않고 로그만 남김
            log.warn("재고 증가 실패는 보상 트랜잭션이므로 처리를 계속합니다.");
        }
    }

    /**
     * 재고 업데이트 결과를 상세히 로깅합니다.
     */
    private void logStockUpdateDetails(StockUpdateResponse response, String operation) {
        if (response.getUpdatedItems() != null) {
            for (StockUpdateResponse.UpdatedStockItem item : response.getUpdatedItems()) {
                log.info("재고 {} 상세 - 상품 ID: {}, 이전 재고: {}, 현재 재고: {}, 변경량: {}",
                    operation, item.getProductId(), item.getPreviousStock(),
                    item.getCurrentStock(), item.getQuantityChange());
            }
        }
    }

    /**
     * 재고 감소 Circuit Breaker 폴백 메서드
     * Product Service가 불가능할 때 즉시 결제 준비를 실패시킵니다.
     */
    public void decreaseStockFallback(Order order, Exception ex) {
        log.error("Circuit breaker activated for decreaseStock - 주문 ID: {}, 매장 ID: {}. Product Service 불가능.",
                order.getOrderId(), order.getStoreId(), ex);
        throw new ApiError("상품 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 재고 증가 Circuit Breaker 폴백 메서드
     * 보상 트랜잭션이므로 예외를 던지지 않고 CRITICAL 로그만 남깁니다.
     */
    public void increaseStockFallback(Order order, Exception ex) {
        log.error("CRITICAL: Circuit breaker activated for increaseStock - 주문 ID: {}, 매장 ID: {}. " +
                "보상 트랜잭션 실패로 수동 개입이 필요합니다.",
                order.getOrderId(), order.getStoreId(), ex);
        // 보상 트랜잭션이므로 예외를 던지지 않음 - 주문 취소 플로우를 중단하지 않기 위함
    }
}
