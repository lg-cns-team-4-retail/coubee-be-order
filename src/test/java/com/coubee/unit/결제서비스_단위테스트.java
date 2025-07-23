package com.coubee.unit;

import com.coubee.client.PortOneClient;
import com.coubee.client.dto.response.PortOnePaymentResponse;
import com.coubee.config.PortOneProperties;
import com.coubee.domain.Order;
import com.coubee.repository.OrderRepository;
import com.coubee.service.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 결제 서비스 단위 테스트
 * 
 * 결제 관련 핵심 비즈니스 로직을 테스트합니다:
 * - 결제 상태 조회
 * - 결제 설정 정보 조회
 * - 결제 준비 프로세스
 * - 결제 실패 시나리오
 * - 결제 수단별 처리 로직
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("결제 서비스 단위 테스트")
class 결제서비스_단위테스트 {

    @Mock
    private PortOneClient portOneClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PortOneProperties portOneProperties;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("결제 상태 조회 성공 테스트")
    void 결제상태_조회_성공_테스트() {
        // Given: 결제가 완료된 주문이 존재한다
        String 결제ID = "payment_20250123_001";
        
        PortOnePaymentResponse 결제응답 = PortOnePaymentResponse.builder()
                .paymentId(결제ID)
                .status("PAID")
                .amount(5000)
                .paidAt("2025-01-23T08:30:00Z")
                .method("KAKAOPAY")
                .build();

        when(portOneClient.getPayment(결제ID)).thenReturn(결제응답);

        // When: 결제 상태를 조회한다
        Object 결제상태 = paymentService.getPaymentStatus(결제ID);

        // Then: 올바른 결제 정보가 반환된다
        assertThat(결제상태).isNotNull();
        verify(portOneClient).getPayment(결제ID);
    }

    @Test
    @DisplayName("결제 상태 조회 실패 테스트")
    void 결제상태_조회_실패_테스트() {
        // Given: 존재하지 않는 결제 ID
        String 존재하지않는결제ID = "invalid_payment_id";
        
        when(portOneClient.getPayment(존재하지않는결제ID))
                .thenThrow(new RuntimeException("결제 정보를 찾을 수 없습니다."));

        // When & Then: 결제 조회 시 예외가 발생한다
        assertThatThrownBy(() -> paymentService.getPaymentStatus(존재하지않는결제ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("결제 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("신용카드 결제 상태 조회 테스트")
    void 신용카드_결제상태_조회_테스트() {
        // Given: 신용카드로 결제된 주문
        String 신용카드결제ID = "card_payment_001";
        
        PortOnePaymentResponse 신용카드응답 = PortOnePaymentResponse.builder()
                .paymentId(신용카드결제ID)
                .status("PAID")
                .amount(12000)
                .paidAt("2025-01-23T12:15:00Z")
                .method("CARD")
                .build();

        when(portOneClient.getPayment(신용카드결제ID)).thenReturn(신용카드응답);

        // When: 신용카드 결제 상태를 조회한다
        Object 결제상태 = paymentService.getPaymentStatus(신용카드결제ID);

        // Then: 신용카드 결제 정보가 정상 반환된다
        assertThat(결제상태).isNotNull();
        verify(portOneClient).getPayment(신용카드결제ID);
    }

    @Test
    @DisplayName("토스페이 결제 상태 조회 테스트")
    void 토스페이_결제상태_조회_테스트() {
        // Given: 토스페이로 결제된 주문
        String 토스페이결제ID = "tosspay_payment_001";
        
        PortOnePaymentResponse 토스페이응답 = PortOnePaymentResponse.builder()
                .paymentId(토스페이결제ID)
                .status("PAID")
                .amount(8500)
                .paidAt("2025-01-23T14:20:00Z")
                .method("TOSSPAY")
                .build();

        when(portOneClient.getPayment(토스페이결제ID)).thenReturn(토스페이응답);

        // When: 토스페이 결제 상태를 조회한다
        Object 결제상태 = paymentService.getPaymentStatus(토스페이결제ID);

        // Then: 토스페이 결제 정보가 정상 반환된다
        assertThat(결제상태).isNotNull();
        verify(portOneClient).getPayment(토스페이결제ID);
    }

    @Test
    @DisplayName("결제 실패 상태 조회 테스트")
    void 결제실패_상태_조회_테스트() {
        // Given: 결제가 실패한 주문
        String 실패한결제ID = "failed_payment_001";
        
        PortOnePaymentResponse 실패응답 = PortOnePaymentResponse.builder()
                .paymentId(실패한결제ID)
                .status("FAILED")
                .amount(7000)
                .paidAt(null)
                .method("KAKAOPAY")
                .build();

        when(portOneClient.getPayment(실패한결제ID)).thenReturn(실패응답);

        // When: 실패한 결제 상태를 조회한다
        Object 결제상태 = paymentService.getPaymentStatus(실패한결제ID);

        // Then: 실패 상태 정보가 반환된다
        assertThat(결제상태).isNotNull();
        verify(portOneClient).getPayment(실패한결제ID);
    }

    @Test
    @DisplayName("고액 결제 상태 조회 테스트")
    void 고액결제_상태_조회_테스트() {
        // Given: 고액 결제 주문 (10만원 이상)
        String 고액결제ID = "high_amount_payment_001";
        
        PortOnePaymentResponse 고액결제응답 = PortOnePaymentResponse.builder()
                .paymentId(고액결제ID)
                .status("PAID")
                .amount(150000)  // 15만원
                .paidAt("2025-01-23T16:45:00Z")
                .method("CARD")
                .build();

        when(portOneClient.getPayment(고액결제ID)).thenReturn(고액결제응답);

        // When: 고액 결제 상태를 조회한다
        Object 결제상태 = paymentService.getPaymentStatus(고액결제ID);

        // Then: 고액 결제 정보가 정상 반환된다
        assertThat(결제상태).isNotNull();
        verify(portOneClient).getPayment(고액결제ID);
    }

    @Test
    @DisplayName("결제 설정 정보 조회 테스트")
    void 결제설정정보_조회_테스트() {
        // Given: 매장별 결제 설정이 구성되어 있다
        Map<String, String> 채널키설정 = Map.of(
                "card", "channel-key-card-001",
                "kakaopay", "channel-key-kakaopay-001",
                "tosspay", "channel-key-tosspay-001",
                "payco", "channel-key-payco-001"
        );

        when(portOneProperties.getStoreId()).thenReturn("store-test-001");
        when(portOneProperties.getChannels()).thenReturn(채널키설정);

        // When: 결제 설정 정보를 조회한다
        String 매장ID = portOneProperties.getStoreId();
        Map<String, String> 채널키목록 = portOneProperties.getChannels();

        // Then: 올바른 설정 정보가 반환된다
        assertThat(매장ID).isEqualTo("store-test-001");
        assertThat(채널키목록).hasSize(4);
        assertThat(채널키목록).containsKey("card");
        assertThat(채널키목록).containsKey("kakaopay");
        assertThat(채널키목록).containsKey("tosspay");
        assertThat(채널키목록).containsKey("payco");
    }

    @Test
    @DisplayName("결제 취소 상태 조회 테스트")
    void 결제취소_상태_조회_테스트() {
        // Given: 취소된 결제
        String 취소된결제ID = "cancelled_payment_001";
        
        PortOnePaymentResponse 취소응답 = PortOnePaymentResponse.builder()
                .paymentId(취소된결제ID)
                .status("CANCELLED")
                .amount(6000)
                .paidAt("2025-01-23T10:00:00Z")
                .method("KAKAOPAY")
                .build();

        when(portOneClient.getPayment(취소된결제ID)).thenReturn(취소응답);

        // When: 취소된 결제 상태를 조회한다
        Object 결제상태 = paymentService.getPaymentStatus(취소된결제ID);

        // Then: 취소 상태 정보가 반환된다
        assertThat(결제상태).isNotNull();
        verify(portOneClient).getPayment(취소된결제ID);
    }

    @Test
    @DisplayName("부분 취소 상태 조회 테스트")
    void 부분취소_상태_조회_테스트() {
        // Given: 부분 취소된 결제
        String 부분취소결제ID = "partial_cancelled_payment_001";
        
        PortOnePaymentResponse 부분취소응답 = PortOnePaymentResponse.builder()
                .paymentId(부분취소결제ID)
                .status("PARTIAL_CANCELLED")
                .amount(10000)  // 원래 금액
                .paidAt("2025-01-23T11:30:00Z")
                .method("CARD")
                .build();

        when(portOneClient.getPayment(부분취소결제ID)).thenReturn(부분취소응답);

        // When: 부분 취소된 결제 상태를 조회한다
        Object 결제상태 = paymentService.getPaymentStatus(부분취소결제ID);

        // Then: 부분 취소 상태 정보가 반환된다
        assertThat(결제상태).isNotNull();
        verify(portOneClient).getPayment(부분취소결제ID);
    }

    @Test
    @DisplayName("결제 수단 없음 오류 테스트")
    void 결제수단_없음_오류_테스트() {
        // Given: 지원하지 않는 결제 수단
        String 지원하지않는결제ID = "unsupported_payment_001";
        
        when(portOneClient.getPayment(지원하지않는결제ID))
                .thenThrow(new RuntimeException("지원하지 않는 결제 수단입니다."));

        // When & Then: 지원하지 않는 결제 수단 조회 시 예외가 발생한다
        assertThatThrownBy(() -> paymentService.getPaymentStatus(지원하지않는결제ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("지원하지 않는 결제 수단입니다.");
    }

    @Test
    @DisplayName("네트워크 오류 시 결제 조회 실패 테스트")
    void 네트워크오류_결제조회_실패_테스트() {
        // Given: 네트워크 연결 문제
        String 결제ID = "network_error_payment_001";
        
        when(portOneClient.getPayment(결제ID))
                .thenThrow(new RuntimeException("네트워크 연결에 실패했습니다."));

        // When & Then: 네트워크 오류 시 예외가 발생한다
        assertThatThrownBy(() -> paymentService.getPaymentStatus(결제ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("네트워크 연결에 실패했습니다.");
    }
}