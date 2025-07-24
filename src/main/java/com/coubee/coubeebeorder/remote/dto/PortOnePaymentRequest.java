package com.coubee.coubeebeorder.remote.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortOnePaymentRequest {

    private String paymentId;
    private Integer amount;
    private String orderName;
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
}