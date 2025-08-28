package com.coubee.coubeebeorder.remote.user;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;

@FeignClient(name = "coubee-be-user-service", path = "/backend/users")
public interface UserClient {
    @GetMapping("/me/owned-stores")
    ApiResponseDto<List<Long>> getMyOwnedStoreIds(@RequestHeader("X-Auth-UserId") Long userId);
}
