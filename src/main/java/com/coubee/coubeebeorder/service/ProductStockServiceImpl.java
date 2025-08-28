package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.domain.OrderItem;
import com.coubee.coubeebeorder.remote.product.ProductClient;
import com.coubee.coubeebeorder.remote.product.StockUpdateRequest;
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
            List<StockUpdateRequest.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockUpdateRequest.StockItem.builder()
                            .productId(item.getProductId())
                            .quantityChange(-item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .storeId(order.getStoreId())
                    .items(stockItems)
                    .build();

            // 1. 반환 타입을 ApiResponseDto<String>으로 변경합니다.
            ApiResponseDto<String> response = productClient.updateStock(request, order.getUserId());

            // 2. product-service의 ApiResponseDto에 success 필드가 없으므로, code 값으로 성공 여부를 판단합니다.
            if ("OK".equals(response.getCode())) {
                log.info("재고 감소 성공 - 주문 ID: {}", order.getOrderId());
                // 3. 응답 데이터(data)가 String이므로 상세 로깅은 제거하거나 단순화합니다.
                // logStockUpdateDetails(response.getData(), "감소"); // 이 라인은 제거해야 합니다.
            } else {
                log.error("재고 감소 실패 - 주문 ID: {}, 응답 코드: {}, 메시지: {}",
                        order.getOrderId(), response.getCode(), response.getMessage());
                // 4. 에러 메시지를 응답에서 가져와서 던집니다.
                throw new RuntimeException("재고 감소에 실패했습니다: " + response.getMessage());
            }

        } catch (Exception e) {
            log.error("재고 감소 중 오류 발생 - 주문 ID: {}", order.getOrderId(), e);
            throw e; 
        }
    }

    @Override
    public void increaseStock(Order order) {
        log.info("재고 증가 요청 - 주문 ID: {}, 매장 ID: {}", order.getOrderId(), order.getStoreId());

        try {
            List<StockUpdateRequest.StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockUpdateRequest.StockItem.builder()
                            .productId(item.getProductId())
                            .quantityChange(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .storeId(order.getStoreId())
                    .items(stockItems)
                    .build();

            ApiResponseDto<String> response = productClient.updateStock(request, order.getUserId());

            if ("OK".equals(response.getCode())) {
                log.info("재고 증가 성공 - 주문 ID: {}", order.getOrderId());
            } else {
                log.error("재고 증가 실패 - 주문 ID: {}, 응답 코드: {}, 메시지: {}",
                        order.getOrderId(), response.getCode(), response.getMessage());
                log.warn("재고 증가 실패는 보상 트랜잭션이므로 처리를 계속합니다.");
            }

        } catch (Exception e) {
            log.error("재고 증가 중 오류 발생 - 주문 ID: {}", order.getOrderId(), e);
            log.warn("재고 증가 실패는 보상 트랜잭션이므로 처리를 계속합니다.");
        }
    }

}
