package com.coubee.coubeebeorder.service;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 상점 보안 관련 서비스 구현체
 * (Store security related service implementation)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreSecurityServiceImpl implements StoreSecurityService {

    private final StoreClient storeClient;

    @Override
    public void validateStoreOwner(Long authenticatedUserId, Long storeId) {
        try {
            log.debug("Validating store ownership - userId: {}, storeId: {}", authenticatedUserId, storeId);
            
            // 사용자가 소유한 승인된 스토어 목록을 조회합니다.
            // (사용자가 소유한 승인된 매장 목록을 조회합니다.)
            ApiResponseDto<List<Long>> response = storeClient.getStoresByOwnerIdOnApproved(authenticatedUserId);
            
            if (response == null || response.getData() == null) {
                log.warn("Failed to retrieve store ownership information for userId: {}", authenticatedUserId);
                throw new SecurityException("Unable to verify store ownership");
            }
            
            List<Long> ownedStoreIds = response.getData();
            
            // 요청된 스토어 ID가 사용자가 소유한 스토어 목록에 포함되어 있는지 확인합니다.
            // (요청된 매장 ID가 사용자 소유 매장 목록에 포함되어 있는지 확인합니다.)
            if (!ownedStoreIds.contains(storeId)) {
                log.warn("Access denied - User {} does not own store {}", authenticatedUserId, storeId);
                throw new SecurityException("Access denied: You are not the owner of this store");
            }
            
            log.debug("Store ownership validation successful - userId: {}, storeId: {}", authenticatedUserId, storeId);
            
        } catch (SecurityException e) {
            // 보안 예외는 그대로 다시 던집니다
            throw e;
        } catch (Exception e) {
            log.error("Error validating store ownership - userId: {}, storeId: {}", authenticatedUserId, storeId, e);
            throw new SecurityException("Failed to validate store ownership: " + e.getMessage());
        }
    }
}
