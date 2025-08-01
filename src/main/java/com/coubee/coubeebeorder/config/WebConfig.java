package com.coubee.coubeebeorder.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    // Gateway에서 CORS를 처리하므로 Order 서비스의 CORS 설정은 비활성화
    // 이렇게 하면 Gateway와 Order 서비스 간의 CORS 설정 충돌을 방지할 수 있습니다.

    // 필요시 개별 서비스 테스트를 위해 CORS 설정을 다시 활성화할 수 있습니다:
    // 1. WebMvcConfigurer 구현 추가
    // 2. addCorsMappings 메서드 구현
    // 3. 필요한 import 추가
}