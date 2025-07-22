package com.coubee.client.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Product Detail Response DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    /**
     * Product ID
     */
    private Long id;

    /**
     * Store ID
     */
    private Long storeId;

    /**
     * Product name
     */
    private String name;

    /**
     * Product price
     */
    private Integer price;

    /**
     * Product stock
     */
    private Integer stock;

    /**
     * Product description
     */
    private String description;

    /**
     * Product image URL
     */
    private String imageUrl;

    /**
     * Product active status
     */
    private boolean active;
} 
