package com.coubee.coubeebeorder.util;

import com.coubee.coubeebeorder.common.dto.ApiResponseDto;
import com.coubee.coubeebeorder.remote.store.StoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("!test") // 정식 @SpringBootTest 실행 시에는 동작하지 않도록 설정
public class FeignClientTester implements ApplicationRunner {

    private final StoreClient storeClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long testStoreId = 1037L; // 테스트할 storeId (translation: storeId to test)
        Long testUserId = 1L; // 테스트용 사용자 ID (translation: Test user ID)
        
        log.info("======================================================================");
        log.info("[FEIGN-STARTUP-TEST] 애플리케이션 시작 후 StoreClient 테스트를 시작합니다."); // (translation: Starting StoreClient test after application startup.)
        log.info("[FEIGN-STARTUP-TEST] 테스트할 storeId: {}, userId: {}", testStoreId, testUserId); // (translation: Testing storeId: {}, userId: {})
        log.info("======================================================================");

        try {
            ApiResponseDto<Long> response = storeClient.getOwnerIdByStoreId(testStoreId, testUserId);
            
            log.info("[FEIGN-STARTUP-TEST] FeignClient 호출 성공!");
            if (response != null) {
                log.info("[FEIGN-STARTUP-TEST] -> 응답 코드(Code): {}", response.getCode());
                log.info("[FEIGN-STARTUP-TEST] -> 응답 메시지(Message): {}", response.getMessage());
                log.info("[FEIGN-STARTUP-TEST] -> 응답 데이터(Data): {}", response.getData());
                
                if (response.getData() != null) {
                    log.info("[FEIGN-STARTUP-TEST] >>>>> 결과: 성공! ownerId를 정상적으로 받아왔습니다. <<<<<");
                } else {
                    log.error("[FEIGN-STARTUP-TEST] >>>>> 결과: 실패! 응답은 받았지만 data 필드가 null 입니다. <<<<<");
                }
            } else {
                log.error("[FEIGN-STARTUP-TEST] >>>>> 결과: 실패! FeignClient로부터 받은 응답 객체가 null 입니다. <<<<<");
            }

        } catch (Exception e) {
            log.error("[FEIGN-STARTUP-TEST] >>>>> 결과: 실패! FeignClient 호출 중 예외가 발생했습니다. <<<<<", e);
        }
        
        log.info("======================================================================");
        log.info("[FEIGN-STARTUP-TEST] StoreClient 테스트를 종료합니다.");
        log.info("======================================================================");
    }
}