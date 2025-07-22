package com.coubee.service;

import com.coubee.client.dto.response.PortOnePaymentResponse;
import com.coubee.dto.request.PaymentReadyRequest;
import com.coubee.dto.response.PaymentReadyResponse;

/**
 * Payment Service Interface for processing payment business logic
 */
public interface PaymentService {

    /**
     * Prepare payment for order
     *
     * @param orderId Order ID
     * @param request Payment preparation request
     * @return Payment preparation response
     */
    PaymentReadyResponse preparePayment(String orderId, PaymentReadyRequest request);

    /**
     * Get payment status
     *
     * @param paymentId Payment ID
     * @return Payment status information
     */
    Object getPaymentStatus(String paymentId);

    /**
     * Handle PortOne webhook
     * Update payment status and change order status on successful payment.
     *
     * @param paymentId Payment ID
     * @return Processing result
     */
    boolean handlePaymentWebhook(String paymentId);

    /**
     * Verify payment information from PortOne
     *
     * @param paymentId Payment ID
     * @return PortOne payment response
     */
    PortOnePaymentResponse verifyPayment(String paymentId);
} 
