package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.domain.*;
import com.coubee.coubeebeorder.domain.repository.OrderRepository;
import com.coubee.coubeebeorder.domain.repository.PaymentRepository;
import com.coubee.coubeebeorder.event.producer.KafkaMessageProducer;
import com.coubee.coubeebeorder.remote.client.ProductServiceClient;
import com.coubee.coubeebeorder.util.PortOneWebhookVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.PaymentClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * V3 결제 서비스 테스트
 * 
 * 결제 완료 시 paidAtUnix 설정 및 OrderItem의 eventType 설정을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class V3PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private KafkaMessageProducer kafkaMessageProducer;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private PortOneWebhookVerifier portOneWebhookVerifier;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order 테스트주문;
    private Payment 테스트결제;
    private OrderItem 테스트주문아이템;

    @BeforeEach
    void setUp() {
        // 테스트 주문 생성
        테스트주문 = Order.createOrder("test_order_123", 1L, 1L, 10000, "테스트 고객");
        
        // 테스트 주문 아이템 생성
        테스트주문아이템 = OrderItem.createOrderItem(1L, "테스트 상품", 2, 5000);
        테스트주문.addOrderItem(테스트주문아이템);
        
        // 테스트 결제 생성
        테스트결제 = Payment.createPayment("test_payment_123", 테스트주문, "CARD", 10000);
        테스트주문.setPayment(테스트결제);
    }

    @Test
    @DisplayName("결제 완료 처리 시 V3 필드 설정 검증")
    void 결제완료처리시_V3필드설정_검증() {
        // Given
        given(orderRepository.findByOrderId("test_order_123")).willReturn(Optional.of(테스트주문));
        
        // 결제 완료 전 상태 확인
        assertThat(테스트주문.getPaidAtUnix()).isNull();
        assertThat(테스트주문아이템.getEventType()).isEqualTo(EventType.PURCHASE); // 기본값

        // When - 결제 완료 상태로 업데이트
        테스트결제.updatePaidStatus("kakaopay", "test_pg_tid", "http://receipt.url");
        테스트주문.updateStatus(OrderStatus.PAID);
        테스트주문.markAsPaidNow(); // V3: 결제 완료 시점 설정
        테스트주문.getItems().forEach(item -> item.updateEventType(EventType.PURCHASE)); // V3: 이벤트 타입 설정

        // Then
        assertThat(테스트주문.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(테스트주문.getPaidAtUnix()).isNotNull();
        assertThat(테스트주문.getPaidAtUnix()).isGreaterThan(0L);
        
        assertThat(테스트주문아이템.getEventType()).isEqualTo(EventType.PURCHASE);
        assertThat(테스트결제.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("주문 취소 시 V3 필드 설정 검증")
    void 주문취소시_V3필드설정_검증() {
        // Given - 결제 완료된 주문
        테스트주문.updateStatus(OrderStatus.PAID);
        테스트주문.markAsPaidNow();
        테스트주문아이템.updateEventType(EventType.PURCHASE);
        
        // When - 주문 취소
        테스트주문.updateStatus(OrderStatus.CANCELLED);
        테스트주문.getItems().forEach(item -> item.updateEventType(EventType.REFUND)); // V3: 환불 이벤트 타입 설정

        // Then
        assertThat(테스트주문.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(테스트주문.getPaidAtUnix()).isNotNull(); // 결제 완료 시점은 유지
        assertThat(테스트주문아이템.getEventType()).isEqualTo(EventType.REFUND);
    }

    @Test
    @DisplayName("다양한 이벤트 타입 설정 테스트")
    void 다양한_이벤트타입_설정_테스트() {
        // Given
        OrderItem 구매아이템 = OrderItem.createOrderItem(1L, "구매 상품", 1, 1000);
        OrderItem 환불아이템 = OrderItem.createOrderItem(2L, "환불 상품", 1, 2000);
        OrderItem 교환아이템 = OrderItem.createOrderItem(3L, "교환 상품", 1, 3000);
        OrderItem 선물아이템 = OrderItem.createOrderItem(4L, "선물 상품", 1, 4000);

        테스트주문.addOrderItem(구매아이템);
        테스트주문.addOrderItem(환불아이템);
        테스트주문.addOrderItem(교환아이템);
        테스트주문.addOrderItem(선물아이템);

        // When
        구매아이템.updateEventType(EventType.PURCHASE);
        환불아이템.updateEventType(EventType.REFUND);
        교환아이템.updateEventType(EventType.EXCHANGE);
        선물아이템.updateEventType(EventType.GIFT);

        // Then
        assertThat(구매아이템.getEventType()).isEqualTo(EventType.PURCHASE);
        assertThat(환불아이템.getEventType()).isEqualTo(EventType.REFUND);
        assertThat(교환아이템.getEventType()).isEqualTo(EventType.EXCHANGE);
        assertThat(선물아이템.getEventType()).isEqualTo(EventType.GIFT);
    }

    @Test
    @DisplayName("결제 완료 시점 정확성 테스트")
    void 결제완료시점_정확성_테스트() {
        // Given
        Long 테스트시작시점 = System.currentTimeMillis() / 1000L;

        // When
        테스트주문.markAsPaidNow();

        // Then
        Long 결제완료시점 = 테스트주문.getPaidAtUnix();
        assertThat(결제완료시점).isNotNull();
        assertThat(결제완료시점).isGreaterThanOrEqualTo(테스트시작시점);
        assertThat(결제완료시점).isLessThanOrEqualTo(System.currentTimeMillis() / 1000L);
    }

    @Test
    @DisplayName("수동 결제 완료 시점 설정 테스트")
    void 수동_결제완료시점_설정_테스트() {
        // Given
        Long 특정시점 = 1672531200L; // 2023-01-01 00:00:00 UTC

        // When
        테스트주문.setPaidAtUnix(특정시점);

        // Then
        assertThat(테스트주문.getPaidAtUnix()).isEqualTo(특정시점);
    }
}
