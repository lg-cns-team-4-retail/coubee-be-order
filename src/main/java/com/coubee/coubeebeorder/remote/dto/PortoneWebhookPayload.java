package com.coubee.coubeebeorder.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PortOne V2 웹훅 페이로드 DTO
 * PortOne V2에서 전송하는 웹훅 JSON 데이터를 매핑하는 클래스
 */
@Data
@NoArgsConstructor
public class PortoneWebhookPayload {
    
    /**
     * PortOne V2 거래 고유 ID (tx_id)
     * V1의 'imp_uid'에 해당합니다.
     */
    @JsonProperty("tx_id")
    private String txId;
    
    /**
     * PortOne V2 결제 ID (payment_id)
     * V1의 'merchant_uid'에 해당하며, 우리의 'orderId'와 같습니다.
     */
    @JsonProperty("payment_id")
    private String paymentId;
    
    /**
     * 결제 상태
     * - Paid: 결제 완료
     * - Failed: 결제 실패
     * - Cancelled: 결제 취소
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * 이 클래스를 사용하는 PaymentWebhookController도 함께 수정해야 합니다.
     * payload.getImpUid() -> payload.getTxId()
     * payload.getMerchantUid() -> payload.getPaymentId()
     * 로 변경해야 합니다.
     */
}