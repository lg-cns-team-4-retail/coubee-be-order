package com.coubee.coubeebeorder.remote;

import com.coubee.coubeebeorder.remote.dto.PortOnePaymentCancelRequest;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentRequest;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentCancelResponse;
import com.coubee.coubeebeorder.remote.dto.PortOnePaymentResponse;
import com.coubee.coubeebeorder.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "portone", 
    url = "${portone.api.url}", 
    configuration = FeignConfig.class
)
public interface PortOneClient {
    
    @PostMapping("/payments/prepare")
    PortOnePaymentResponse preparePayment(@RequestBody PortOnePaymentRequest request);

    @GetMapping("/payments/{imp_uid}")
    PortOnePaymentResponse getPayment(@PathVariable("imp_uid") String impUid);

    @PostMapping("/payments/cancel")
    PortOnePaymentCancelResponse cancelPayment(@RequestBody PortOnePaymentCancelRequest request);
}