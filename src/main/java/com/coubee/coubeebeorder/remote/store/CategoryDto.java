package com.coubee.coubeebeorder.remote.store;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [신규] StoreResponseDto의 storeTag 리스트를 받기 위한 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long categoryId;
    private String name;
}
