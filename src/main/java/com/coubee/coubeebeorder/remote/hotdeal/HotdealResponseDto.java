package com.coubee.coubeebeorder.remote.hotdeal;

import lombok.Data;

/**
 * Hotdeal API 응답 DTO
 * 
 * 스토어의 활성 핫딜 정보를 담는 DTO입니다.
 * saleRate는 할인율(0.1 = 10%), maxDiscount는 최대 할인 금액입니다.
 */
@Data
public class HotdealResponseDto {
    
    /**
     * 할인율 (0.1 = 10% 할인)
     */
    private Double saleRate;
    
    /**
     * 최대 할인 금액
     */
    private Integer maxDiscount;
}
