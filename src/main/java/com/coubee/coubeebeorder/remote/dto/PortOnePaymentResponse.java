package com.coubee.coubeebeorder.remote.dto;

import lombok.Data;

@Data
public class PortOnePaymentResponse {
    private String code;
    private String message;
    private PaymentInfo response;

    @Data
    public static class PaymentInfo {
        private String imp_uid;
        private String merchant_uid;
        private String pay_method;
        private String channel;
        private String pg_provider;
        private String emb_pg_provider;
        private String pg_tid;
        private String pg_id;
        private boolean escrow;
        private String apply_num;
        private String bank_code;
        private String bank_name;
        private String card_code;
        private String card_name;
        private Integer card_quota;
        private String vbank_code;
        private String vbank_name;
        private String vbank_num;
        private String vbank_holder;
        private Long vbank_date;
        private String vbank_issued_at;
        private String name;
        private Integer amount;
        private Integer cancel_amount;
        private String currency;
        private String buyer_name;
        private String buyer_email;
        private String buyer_tel;
        private String buyer_addr;
        private String buyer_postcode;
        private String custom_data;
        private String user_agent;
        private String status;
        private Long started_at;
        private Long paid_at;
        private Long failed_at;
        private Long cancelled_at;
        private String fail_reason;
        private String cancel_reason;
        private String receipt_url;
        private String cancel_receipt_urls;
        private Boolean cash_receipt_issued;
        private String customer_uid;
        private String customer_uid_usage;
    }
}