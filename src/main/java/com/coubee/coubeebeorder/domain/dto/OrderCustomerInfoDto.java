package com.coubee.coubeebeorder.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 응답에 포함될 고객 정보 DTO
 * (Customer information DTO to be included in order response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCustomerInfoDto {

    /**
     * 사용자명
     * (Username)
     */
    private String username;

    /**
     * 닉네임
     * (Nickname)
     */
    private String nickname;

    /**
     * 실명
     * (Real name)
     */
    private String name;

    /**
     * 이메일 주소
     * (Email address)
     */
    private String email;

    /**
     * 전화번호
     * (Phone number)
     */
    private String phoneNum;
}
