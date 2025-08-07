package com.coubee.coubeebeorder.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PortOne 최신 웹훅 페이로드 DTO (2024-04-25 버전 기준)
 * 중첩된 'data' 객체 구조를 포함합니다.
 */
@Data
@NoArgsConstructor
public class PortoneWebhookPayload {

    @JsonProperty("type")
    private String type;

    @JsonProperty("timestamp")
    private String timestamp;

    // ✅✅✅ 이 부분이 핵심입니다. 'WebhookData'라는 내부 클래스를 정의하고,
    // 'data' 필드가 이 클래스 타입을 갖도록 합니다. ✅✅✅
    @JsonProperty("data")
    private WebhookData data;

    @Data
    @NoArgsConstructor
    public static class WebhookData {
        
        @JsonProperty("storeId")
        private String storeId;
        
        /**
         * 포트원에서 채번한 고유 결제 시도 번호
         * V1의 imp_uid에 해당
         */
        @JsonProperty("transactionId")
        private String transactionId;

        /**
         * 고객사에서 채번한 결제 건의 고유 주문 번호
         * V1의 merchant_uid에 해당
         */
        @JsonProperty("paymentId")
        private String paymentId;
        
        @JsonProperty("cancellationId")
        private String cancellationId;
    }
}