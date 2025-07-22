package com.coubee.client.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Product ID List Request DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductIdsRequest {

    /**
     * Product ID List
     */
    private List<Long> productIds;
} 
