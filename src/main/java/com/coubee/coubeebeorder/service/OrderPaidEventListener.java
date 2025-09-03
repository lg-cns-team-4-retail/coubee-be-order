package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.kafka.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.kafka.producer.notification.event.OrderNotificationEvent;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import com.coubee.coubeebeorder.remote.store.StoreResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidEventListener {

    private final StoreClient storeClient;
    private final KafkaMessageProducer kafkaMessageProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        log.info("주문 생성 트랜잭션 커밋 완료. 점주 알림을 시작합니다. Order ID: {}", event.orderId()); // (translation: Order creation transaction committed. Starting owner notification. Order ID:)
        log.info("주문 생성 트랜잭션 커밋 완료. 점주 알림을 시작합니다. Order ID: {}", event.storeId()); // (translation: Order creation transaction committed. Starting owner notification. Order ID:)

        log.debug("이벤트 데이터: storeId={}, userId={}", event.storeId(), event.userId()); // (translation: Event data: storeId={}, userId={})
        
        try {
            // 이제 이 로직은 주문이 DB에 완전히 저장된 후에 실행되므로 안전합니다. (translation: This logic is now safe to execute as the order has been fully saved to the DB.)
            ApiResponseDto<StoreResponseDto> storeResponse = storeClient.getStoreById(event.storeId(), event.userId());
            String storeName = storeResponse.getData() != null ? storeResponse.getData().getStoreName() : "매장"; // (translation: Store)

            log.debug("점주 ID 조회 시작 - storeId: {}", event.storeId()); // (translation: Starting owner ID lookup - storeId: {})
            ApiResponseDto<Long> ownerIdResponse = storeClient.getOwnerIdByStoreId(event.storeId());
            log.debug("점주 ID 조회 응답: {}", ownerIdResponse); // (translation: Owner ID lookup response: {})

            if (ownerIdResponse != null && ownerIdResponse.isSuccess() && ownerIdResponse.getData() != null) {
                Long ownerId = ownerIdResponse.getData();
                OrderNotificationEvent forOwner = OrderNotificationEvent.createNewOrderNotificationForOwner(
                        event.orderId(), ownerId, storeName);
                kafkaMessageProducer.publishOrderNotificationEvent(forOwner);
                log.info("점주에게 신규 주문 알림 발행 성공. Order: {}, Owner ID: {}", event.orderId(), ownerId); // (translation: Successfully published new order notification to owner. Order: {}, Owner ID: {})
            } else {
                // 더 자세한 오류 정보 로깅 (translation: More detailed error logging)
                if (ownerIdResponse == null) {
                    log.error("점주 ID 조회 실패: 응답이 null입니다. StoreId: {}, UserId: {}", event.storeId(), event.userId()); // (translation: Owner ID lookup failed: response is null. StoreId: {}, UserId: {})
                } else if (!ownerIdResponse.isSuccess()) {
                    log.error("점주 ID 조회 실패: API 응답 실패. StoreId: {}, UserId: {}, Code: {}, Message: {}", 
                            event.storeId(), event.userId(), ownerIdResponse.getCode(), ownerIdResponse.getMessage()); // (translation: Owner ID lookup failed: API response failed. StoreId: {}, UserId: {}, Code: {}, Message: {})
                } else {
                    log.error("점주 ID 조회 실패: 데이터가 null입니다. StoreId: {}, UserId: {}", event.storeId(), event.userId()); // (translation: Owner ID lookup failed: data is null. StoreId: {}, UserId: {})
                }
                log.error("알림 미발송. StoreId: {}", event.storeId()); // (translation: Notification not sent. StoreId:)
            }
        } catch (Exception e) {
            // 이 로직에서 에러가 발생해도, 이미 주문 생성 트랜잭션은 성공했으므로 롤백되지 않습니다. (translation: Even if an error occurs in this logic, the order creation transaction has already succeeded and will not be rolled back.)
            log.error("점주 알림 발행 중 최종 예외 발생. Order: {}", event.orderId(), e); // (translation: Final exception occurred while publishing owner notification. Order:)
        }
    }
}
