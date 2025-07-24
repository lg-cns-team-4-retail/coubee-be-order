package com.coubee.coubeebeorder.remote.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortOnePaymentCancelRequest {
    private String imp_uid;
    private String merchant_uid;
    private Integer amount;
    private Integer tax_free;
    private Integer vat_amount;
    private String reason;
    private String refund_holder;
    private String refund_bank;
    private String refund_account;
    private Integer refund_tel;
}