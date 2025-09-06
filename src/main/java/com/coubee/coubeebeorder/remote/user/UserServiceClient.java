package com.coubee.coubeebeorder.remote.user;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * User 서비스와 통신하기 위한 Feign 클라이언트
 */
@FeignClient(
    name = "coubee-be-user-service",
    url = "http://coubee-be-user-service:8080",
    configuration = {com.coubee.coubeebeorder.config.FeignConfig.class, com.coubee.coubeebeorder.config.UserClientErrorDecoder.class}
)
public interface UserServiceClient {

    /**
     * 사용자 ID로 사용자 정보 조회
     *
     * @param userId 조회할 사용자의 ID
     * @return SiteUserInfoDto를 포함한 ApiResponseDto
     */
    @GetMapping("/backend/user/user/{userId}")
    ApiResponseDto<SiteUserInfoDto> getUserInfo(@PathVariable("userId") Long userId);

    /**
     * 사용자 ID(Long)로 사용자 정보를 조회합니다
     * User 서비스의 '/backend/user/info/{userId}' 엔드포인트를 호출합니다
     *
     * @param userId 조회할 사용자의 ID (Long)
     * @return SiteUserInfoDto를 포함한 ApiResponseDto
     */
    @GetMapping("/backend/user/info/{userId}")
    ApiResponseDto<SiteUserInfoDto> getUserInfoById(@PathVariable("userId") Long userId);

}
