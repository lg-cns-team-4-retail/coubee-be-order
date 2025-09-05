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
        log.info("주문 생성 트랜잭션 커밋 완료. 점주 알림을 시작합니다. Order ID: {}", event.orderId());
        log.info("주문 생성 트랜잭션 커밋 완료. 점주 알림을 시작합니다. Store ID: {}", event.storeId());

        log.debug("이벤트 데이터: storeId={}, userId={}", event.storeId(), event.userId());
        
        try {
            // 이제 이 로직은 주문이 DB에 완전히 저장된 후에 실행되므로 안전합니다.
            ApiResponseDto<StoreResponseDto> storeResponse = storeClient.getStoreById(event.storeId(), event.userId());
            String storeName = storeResponse.getData() != null ? storeResponse.getData().getStoreName() : "매장";
            


            
            log.debug("점주 ID 조회 시작 - storeId: {}", event.storeId());
            ApiResponseDto<Long> ownerIdResponse = storeClient.getOwnerIdByStoreId(event.storeId());
            log.info("test :{}",ownerIdResponse.getCode());
            log.info("test :{}",ownerIdResponse.getData());
            log.info("test: {}", ownerIdResponse.getMessage());
            log.debug("점주 ID 조회 응답: {}", ownerIdResponse);

            if (ownerIdResponse != null && "OK".equals(ownerIdResponse.getCode()) && ownerIdResponse.getData() != null) {
                Long ownerId = ownerIdResponse.getData();
                OrderNotificationEvent forOwner = OrderNotificationEvent.createNewOrderNotificationForOwner(
                        event.orderId(), ownerId, storeName, event.storeId());
                kafkaMessageProducer.publishOrderNotificationEvent(forOwner);
                log.info("점주에게 신규 주문 알림 발행 성공. Order: {}, Owner ID: {}", event.orderId(), ownerId);
            } else {
                // 더 자세한 오류 정보 로깅
                if (ownerIdResponse == null) {
                    log.error("점주 ID 조회 실패: 응답이 null입니다. StoreId: {}, UserId: {}", event.storeId(), event.userId());
                } else if (!"OK".equals(ownerIdResponse.getCode())) {
                    log.error("점주 ID 조회 실패: API 응답 실패. StoreId: {}, UserId: {}, Code: {}, Message: {}", 
                            event.storeId(), event.userId(), ownerIdResponse.getCode(), ownerIdResponse.getMessage());
                } else {
                    log.error("점주 ID 조회 실패: 데이터가 null입니다. StoreId: {}, UserId: {}", event.storeId(), event.userId());
                }
                log.error("알림 미발송. StoreId: {}", event.storeId());
            }
        } catch (Exception e) {
            // 이 로직에서 에러가 발생해도, 이미 주문 생성 트랜잭션은 성공했으므로 롤백되지 않습니다.
            log.error("점주 알림 발행 중 최종 예외 발생. Order: {}", event.orderId(), e);
        }
    }
}
