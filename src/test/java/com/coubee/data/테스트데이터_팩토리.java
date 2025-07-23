package com.coubee.data;

import com.coubee.domain.*;
import com.coubee.dto.request.OrderCreateRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 테스트 데이터 팩토리
 * 
 * 테스트에 필요한 다양한 데이터를 생성하는 유틸리티 클래스입니다.
 * - 주문 관련 테스트 데이터
 * - 결제 관련 테스트 데이터
 * - 사용자 관련 테스트 데이터
 * - 상품 관련 테스트 데이터
 */
public class 테스트데이터_팩토리 {

    private static final Random random = new Random();
    
    // 테스트용 고객 이름 목록
    private static final String[] 고객이름목록 = {
        "김철수", "이영희", "박민수", "정다은", "최준호", "한지민",
        "강태희", "윤서연", "임현우", "송지아", "조민석", "허예린"
    };
    
    // 테스트용 결제 수단 목록
    public static class 결제수단 {
        public static final String 카드 = "CARD";
        public static final String 카카오페이 = "KAKAOPAY";
        public static final String 토스페이 = "TOSSPAY";
        public static final String 페이코 = "PAYCO";
        
        public static String 랜덤_결제수단() {
            String[] 결제수단들 = {카드, 카카오페이, 토스페이, 페이코};
            return 결제수단들[random.nextInt(결제수단들.length)];
        }
    }
    
    // 테스트용 상품 정보
    public static class 상품정보 {
        public static final Long 상품1_ID = 1L;
        public static final String 상품1_이름 = "테스트 상품 1";
        public static final Integer 상품1_가격 = 100;
        
        public static final Long 상품2_ID = 2L;
        public static final String 상품2_이름 = "테스트 상품 2";
        public static final Integer 상품2_가격 = 100;
        
        public static final Long 상품3_ID = 3L;
        public static final String 상품3_이름 = "테스트 상품 3";
        public static final Integer 상품3_가격 = 100;
    }
    
    // 테스트용 매장 정보
    public static class 매장정보 {
        public static final Long 매장1_ID = 1L;
        public static final String 매장1_이름 = "테스트 매장 1";
        
        public static final Long 매장2_ID = 2L;
        public static final String 매장2_이름 = "테스트 매장 2";
        
        public static final Long 매장3_ID = 3L;
        public static final String 매장3_이름 = "테스트 매장 3";
    }
    
    /**
     * 랜덤한 고객 이름 생성
     */
    public static String 랜덤_고객이름() {
        return 고객이름목록[random.nextInt(고객이름목록.length)];
    }
    
    /**
     * 기본 주문 요청 생성
     */
    public static OrderCreateRequest 기본_주문요청() {
        return OrderCreateRequest.builder()
                .storeId(매장정보.매장1_ID)
                .recipientName("테스트고객")
                .paymentMethod(결제수단.카드)
                .items(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(상품정보.상품1_ID)
                                .quantity(1)
                                .build()
                ))
                .build();
    }
    
    /**
     * 단일 상품 주문 요청 생성
     */
    public static OrderCreateRequest 단일상품_주문요청(Long 상품ID, int 수량, String 결제수단) {
        return OrderCreateRequest.builder()
                .storeId(매장정보.매장1_ID)
                .recipientName(랜덤_고객이름())
                .paymentMethod(결제수단)
                .items(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(상품ID)
                                .quantity(수량)
                                .build()
                ))
                .build();
    }
    
    /**
     * 다중 상품 주문 요청 생성
     */
    public static OrderCreateRequest 다중상품_주문요청() {
        return OrderCreateRequest.builder()
                .storeId(매장정보.매장1_ID)
                .recipientName(랜덤_고객이름())
                .paymentMethod(결제수단.랜덤_결제수단())
                .items(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(상품정보.상품1_ID)
                                .quantity(2)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(상품정보.상품2_ID)
                                .quantity(1)
                                .build()
                ))
                .build();
    }
    
    /**
     * 대량 주문 요청 생성
     */
    public static OrderCreateRequest 대량_주문요청(int 수량) {
        return OrderCreateRequest.builder()
                .storeId(매장정보.매장1_ID)
                .recipientName("대량주문고객")
                .paymentMethod(결제수단.카드)
                .items(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(상품정보.상품1_ID)
                                .quantity(수량)
                                .build()
                ))
                .build();
    }
    
    /**
     * 결제수단별 주문 요청 생성
     */
    public static class 결제수단별_주문요청 {
        
        public static OrderCreateRequest 카드결제_주문() {
            return 단일상품_주문요청(상품정보.상품1_ID, 1, 결제수단.카드);
        }
        
        public static OrderCreateRequest 카카오페이_주문() {
            return 단일상품_주문요청(상품정보.상품1_ID, 1, 결제수단.카카오페이);
        }
        
        public static OrderCreateRequest 토스페이_주문() {
            return 단일상품_주문요청(상품정보.상품1_ID, 1, 결제수단.토스페이);
        }
        
        public static OrderCreateRequest 페이코_주문() {
            return 단일상품_주문요청(상품정보.상품1_ID, 1, 결제수단.페이코);
        }
    }
    
    /**
     * 주문 엔티티 생성
     */
    public static Order 테스트_주문_엔티티() {
        String 주문ID = "order_" + System.currentTimeMillis();
        Order 주문 = Order.createOrder(
                주문ID,
                random.nextLong(100000) + 1,  // 사용자 ID
                매장정보.매장1_ID,
                상품정보.상품1_가격,
                랜덤_고객이름()
        );
        
        // QR 토큰 설정
        String qr토큰 = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(주문ID.getBytes());
        주문.setOrderToken(qr토큰);
        
        return 주문;
    }
    
    /**
     * 결제 엔티티 생성
     */
    public static Payment 테스트_결제_엔티티(Order 주문) {
        return Payment.createPayment(
                주문.getOrderId(),
                주문,
                결제수단.랜덤_결제수단(),
                주문.getTotalAmount()
        );
    }
    
    /**
     * 주문 아이템 엔티티 생성
     */
    public static OrderItem 테스트_주문아이템_엔티티(Order 주문) {
        return OrderItem.createOrderItem(
                주문,
                상품정보.상품1_ID,
                상품정보.상품1_이름,
                1,
                상품정보.상품1_가격
        );
    }
    
    /**
     * 특수 상황별 주문 요청
     */
    public static class 특수상황_주문요청 {
        
        public static OrderCreateRequest 취소예정_주문() {
            return OrderCreateRequest.builder()
                    .storeId(매장정보.매장1_ID)
                    .recipientName("취소예정고객")
                    .paymentMethod(결제수단.카드)
                    .items(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId(상품정보.상품1_ID)
                                    .quantity(1)
                                    .build()
                    ))
                    .build();
        }
        
        public static OrderCreateRequest 고액_주문(int 수량) {
            return OrderCreateRequest.builder()
                    .storeId(매장정보.매장1_ID)
                    .recipientName("고액주문고객")
                    .paymentMethod(결제수단.카드)
                    .items(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId(상품정보.상품1_ID)
                                    .quantity(수량)
                                    .build()
                    ))
                    .build();
        }
        
        public static OrderCreateRequest 수령대기_주문() {
            return OrderCreateRequest.builder()
                    .storeId(매장정보.매장1_ID)
                    .recipientName("수령대기고객")
                    .paymentMethod(결제수단.카카오페이)
                    .items(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId(상품정보.상품1_ID)
                                    .quantity(1)
                                    .build()
                    ))
                    .build();
        }
    }
    
    /**
     * 성능 테스트용 대량 데이터 생성
     */
    public static class 성능테스트_데이터 {
        
        public static OrderCreateRequest 동시접속_주문(int 사용자번호) {
            return OrderCreateRequest.builder()
                    .storeId(매장정보.매장1_ID)
                    .recipientName("동시접속고객" + 사용자번호)
                    .paymentMethod(결제수단.랜덤_결제수단())
                    .items(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId(상품정보.상품1_ID)
                                    .quantity(1)
                                    .build()
                    ))
                    .build();
        }
        
        public static OrderCreateRequest 부하테스트_주문(Long 사용자ID) {
            return OrderCreateRequest.builder()
                    .storeId(매장정보.매장1_ID)
                    .recipientName("부하테스트고객_" + 사용자ID)
                    .paymentMethod(결제수단.카드)
                    .items(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId(상품정보.상품1_ID)
                                    .quantity(random.nextInt(5) + 1)  // 1-5개 랜덤
                                    .build()
                    ))
                    .build();
        }
    }
    
    /**
     * 시간 관련 테스트 데이터
     */
    public static class 시간관련_데이터 {
        
        public static LocalDateTime 현재시간() {
            return LocalDateTime.now();
        }
        
        public static LocalDateTime 과거시간(int 일수) {
            return LocalDateTime.now().minusDays(일수);
        }
        
        public static LocalDateTime 미래시간(int 일수) {
            return LocalDateTime.now().plusDays(일수);
        }
    }
}