package com.coubee.coubeebeorder.remote.dto;

import lombok.Data;

@Data
public class PortOnePaymentCancelResponse {
    private String code;
    private String message;
    private CancelInfo response;

    @Data
    public static class CancelInfo {
        private String imp_uid;
        private String merchant_uid;
        private Integer amount;
        private Integer cancel_amount;
        private String currency;
        private String status;
        private String cancel_reason;
        private Long cancelled_at;
        private String receipt_url;
    }
}