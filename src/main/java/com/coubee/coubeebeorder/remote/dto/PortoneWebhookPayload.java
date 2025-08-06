package com.coubee.coubeebeorder.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PortOne 웹훅 페이로드 DTO
 * PortOne에서 전송하는 웹훅 JSON 데이터를 매핑하는 클래스
 */
@Data
@NoArgsConstructor
public class PortoneWebhookPayload {
    
    /**
     * PortOne 결제 고유 ID (imp_uid)
     */
    @JsonProperty("imp_uid")
    private String impUid;
    
    /**
     * 가맹점 주문 ID (merchant_uid)
     */
    @JsonProperty("merchant_uid")
    private String merchantUid;
    
    /**
     * 결제 상태
     * - paid: 결제 완료
     * - failed: 결제 실패
     * - cancelled: 결제 취소
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * 결제 금액
     */
    @JsonProperty("amount")
    private Long amount;
    
    /**
     * 결제 방법
     */
    @JsonProperty("pay_method")
    private String payMethod;
    
    /**
     * 결제 완료 시각 (Unix timestamp)
     */
    @JsonProperty("paid_at")
    private Long paidAt;
    
    /**
     * 실패 사유
     */
    @JsonProperty("fail_reason")
    private String failReason;
    
    /**
     * 취소 사유
     */
    @JsonProperty("cancel_reason")
    private String cancelReason;
}
