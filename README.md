# Coubee Order Payment Service

결제 기능이 통합된 주문 관리 마이크로서비스입니다.

## 주요 기능

- 주문 생성 및 결제
- 주문 조회 및 관리
- 주문 취소 및 환불

## API 문서

서버 실행 후 아래 URL에서 Swagger API 문서를 확인할 수 있습니다:
```
http://localhost:8080/swagger-ui.html
```

## 로컬 개발 환경 설정

### 필수 요구사항

- Java 17 이상
- MySQL 8.x
- Kafka

### 환경 변수

다음 환경 변수를 설정하거나 `application.yml`에서 기본값을 사용할 수 있습니다:

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=coubee_order
DB_USERNAME=root
DB_PASSWORD=root

KAFKA_SERVERS=localhost:9092

PORTONE_API_URL=https://api.portone.io
PORTONE_API_SECRET=your_api_secret
```

## 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 실행
java -jar build/libs/coubee-order-payment-service-0.0.1-SNAPSHOT.jar
```

## 데이터베이스 스키마

데이터베이스 스키마는 Flyway를 통해 자동으로 생성됩니다. 초기 스키마는 `src/main/resources/db/migration/V1__init_schema.sql` 파일에 정의되어 있습니다.

## Kafka 토픽

이 서비스는 다음 Kafka 토픽을 사용합니다:

- `stock-decrease`: 재고 감소 이벤트 발행
- `stock-increase`: 재고 증가 이벤트 발행
- `notification-create`: 알림 생성 이벤트 발행

## 외부 서비스 연동

- **PortOne**: 결제 처리를 위한 PG 서비스
- **Product Service**: 상품 정보 조회를 위한 내부 서비스