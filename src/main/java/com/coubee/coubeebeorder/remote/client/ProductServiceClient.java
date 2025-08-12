package com.coubee.coubeebeorder.remote.client;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.remote.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for communicating with the Product Service
 * 
 * In Kubernetes environment, this client will communicate with the product service
 * using the service name 'coubee-be-product-service' which is resolved by Kubernetes DNS.
 * 
 * The X-Auth-UserId header is required by the product service endpoint and must be
 * forwarded from the original request.
 */
@FeignClient(
    name = "product-service",
    url = "http://coubee-be-product-service:8080",
    configuration = com.coubee.coubeebeorder.config.FeignConfig.class
)
public interface ProductServiceClient {
    
    /**
     * Get product details by product ID
     * 
     * @param productId The ID of the product to retrieve
     * @param userId The user ID from the X-Auth-UserId header (required by product service)
     * @return ApiResponseDto containing ProductResponseDto
     */
    @GetMapping("/api/product/detail/{productId}")
    ApiResponseDto<ProductResponseDto> getProductById(
            @PathVariable("productId") Long productId,
            @RequestHeader("X-Auth-UserId") Long userId
    );
}
