package com.coubee.coubeebeorder.remote.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User 서비스에서 반환하는 사용자 정보 DTO
 * (User information DTO returned from the User service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteUserInfoDto {

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

    /**
     * 성별
     * (Gender)
     */
    private String gender;

    /**
     * 나이
     * (Age)
     */
    private Integer age;

    /**
     * 프로필 이미지 URL
     * (Profile image URL)
     */
    private String profileImageUrl;

    /**
     * 정보 등록 여부
     * (Whether information is registered)
     */
    private Boolean isInfoRegister;
}
