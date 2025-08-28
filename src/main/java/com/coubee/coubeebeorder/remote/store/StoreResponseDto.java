package com.coubee.coubeebeorder.remote.store;

import lombok.Data;

/**
 * Store response DTO that matches the main branch production API contract
 * Simplified version without advanced fields for production stability
 */
@Data
public class StoreResponseDto {

    private Long storeId;

    private String storeName;

    private String description;

    private String storeAddress;

    private String contactNo;

    private String workingHour;

    // Fallback flag to indicate if this data is from fallback mechanism
    private boolean fallback = false;

    // store-service 응답에는 ownerId가 없으므로 제거
    // private Long ownerId;
}
