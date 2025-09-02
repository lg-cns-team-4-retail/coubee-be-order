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
            // (Retrieves the list of approved stores owned by the user.)
            ApiResponseDto<List<Long>> response = storeClient.getStoresByOwnerIdOnApproved(authenticatedUserId);
            
            if (response == null || response.getData() == null) {
                log.warn("Failed to retrieve store ownership information for userId: {}", authenticatedUserId);
                throw new SecurityException("Unable to verify store ownership");
            }
            
            List<Long> ownedStoreIds = response.getData();
            
            // 요청된 스토어 ID가 사용자가 소유한 스토어 목록에 포함되어 있는지 확인합니다.
            // (Checks if the requested store ID is included in the user's owned store list.)
            if (!ownedStoreIds.contains(storeId)) {
                log.warn("Access denied - User {} does not own store {}", authenticatedUserId, storeId);
                throw new SecurityException("Access denied: You are not the owner of this store");
            }
            
            log.debug("Store ownership validation successful - userId: {}, storeId: {}", authenticatedUserId, storeId);
            
        } catch (SecurityException e) {
            // Re-throw security exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Error validating store ownership - userId: {}, storeId: {}", authenticatedUserId, storeId, e);
            throw new SecurityException("Failed to validate store ownership: " + e.getMessage());
        }
    }
}
