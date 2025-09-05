package com.coubee.coubeebeorder.service;

// PortOnePaymentResponse 대신 SDK의 DTO를 가져옵니다
// import io.portone.sdk.server.payment.PaymentGetResponse;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyRequest;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyResponse;

public interface PaymentService {
    PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request);

    // 반환 타입을 Object로 변경합니다 (SDK의 다양한 PaymentGetResponse 서브타입 지원)
    Object getPaymentStatus(String paymentId);

    // 시그니처를 webhookId, signature, timestamp, requestBody를 받도록 수정합니다
    boolean handlePaymentWebhook(String webhookId, String signature, String timestamp, String requestBody);

    // 반환 타입을 SDK의 DTO로 변경합니다
    Object verifyPayment(String paymentId);

    /**
     * 테스트 주문을 생성하고 완료 상태로 만든 후 Kafka 이벤트를 발행합니다.
     *
     * @param request userId, storeId, productId, quantity를 포함한 요청 데이터
     * @return 새로 생성된 테스트 주문의 orderId
     */
    String createAndCompleteTestOrder(com.coubee.coubeebeorder.domain.dto.TestOrderCreateRequest request);
}
