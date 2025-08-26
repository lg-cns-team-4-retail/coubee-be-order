package com.coubee.coubeebeorder.remote.store;

import lombok.Data;

/**
 * Store response DTO that matches the store service response structure
 */
@Data
public class StoreResponseDto {
    
    private Long storeId;
    
    private String storeName;
    
    private String description;
    
    private String storeImg;
    
    private String address;
    
    private String phoneNumber;
    
    private String businessHours;
    
    private String category;
    
    private Long ownerId;
}
