package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.remote.dto.PortOnePaymentResponse;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyRequest;
import com.coubee.coubeebeorder.domain.dto.PaymentReadyResponse;

public interface PaymentService {

    PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request);

    Object getPaymentStatus(String paymentId);

    boolean handlePaymentWebhook(String paymentId);

    PortOnePaymentResponse verifyPayment(String paymentId);
}