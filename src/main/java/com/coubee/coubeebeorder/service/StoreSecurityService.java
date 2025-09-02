package com.coubee.coubeebeorder.service;

/**
 * 상점 보안 관련 서비스 인터페이스
 * (Store security related service interface)
 */
public interface StoreSecurityService {
    
    /**
     * 인증된 사용자가 해당 상점의 소유자인지 검증합니다.
     * (Validates if the authenticated user is the owner of the store.)
     * 
     * @param authenticatedUserId 인증된 사용자 ID
     * @param storeId 상점 ID
     * @throws SecurityException 사용자가 상점의 소유자가 아닌 경우
     */
    void validateStoreOwner(Long authenticatedUserId, Long storeId);
}
