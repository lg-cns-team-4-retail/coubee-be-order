package com.coubee.coubeebeorder.remote.product;

import lombok.Data;

/**
 * Product response DTO that matches the product service response structure
 */
@Data
public class ProductResponseDto {
    
    private Long productId;
    
    private String productName;
    
    private String description;
    
    private String productImg;
    
    private int originPrice;
    
    private int salePrice;
    
    private int stock;
    
    private Long storeId;
}
