package com.coubee.coubeebeorder.remote.store;

import lombok.Data;
import java.util.List;

/**
 * Store response DTO that matches the store service response structure
 * [수정됨] store-service의 실제 응답과 필드명을 일치시켰습니다.
 */
@Data
public class StoreResponseDto {
    
    private Long storeId;
    
    private String storeName;
    
    private String description;
    
    // storeImg -> profileImg 로 변경
    private String profileImg;

    // backImg 필드 추가
    private String backImg;
    
    // address -> storeAddress 로 변경
    private String storeAddress;
    
    // phoneNumber -> contactNo 로 변경
    private String contactNo;
    
    // businessHours -> workingHour 로 변경
    private String workingHour;
    
    // category(String) -> storeTag(List<CategoryDto>) 로 변경
    private List<CategoryDto> storeTag;
    
    // store-service 응답에는 ownerId가 없으므로 제거
    // private Long ownerId;
}
