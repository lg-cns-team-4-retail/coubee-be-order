package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.remote.product.ProductClient;
import com.coubee.coubeebeorder.remote.product.StockUpdateRequest;
import com.coubee.coubeebeorder.remote.product.StockUpdateResponse;
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
}
