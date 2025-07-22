package com.coubee.client;

import com.coubee.client.dto.request.ProductIdsRequest;
import com.coubee.client.dto.response.ProductDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product Service API Client
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /**
     * Get product information by product ID list
     *
     * @param storeId Store ID
     * @param request Product ID list request
     * @return Product information list
     */
    @PostMapping("/api/internal/stores/{storeId}/products/batch")
    List<ProductDetailResponse> getProductsByIds(
            @PathVariable("storeId") Long storeId,
            @RequestBody ProductIdsRequest request);

    /**
     * Get single product information
     *
     * @param productId Product ID
     * @return Product information
     */
    @GetMapping("/api/internal/products/{productId}")
    ProductDetailResponse getProduct(@PathVariable("productId") Long productId);
} 
