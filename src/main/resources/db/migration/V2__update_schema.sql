-- V2: 스키마 업데이트 - Orders 및 Payments 테이블 구조 변경

-- =================================================================================
-- Orders 테이블 구조 변경
-- =================================================================================

-- 1. Orders 테이블에 새로운 컬럼들 추가
ALTER TABLE orders
ADD COLUMN store_id BIGINT NOT NULL DEFAULT 1,
ADD COLUMN recipient_name VARCHAR(255) NOT NULL DEFAULT 'Unknown',
ADD COLUMN order_token VARCHAR(255) NULL,
ADD COLUMN order_qr TEXT NULL;

-- 2. Orders 테이블 컬럼명 변경: order_status → status
ALTER TABLE orders RENAME COLUMN order_status TO status;
ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(20);

-- 3. 불필요한 컬럼들 제거
ALTER TABLE orders DROP COLUMN IF EXISTS shipping_address;
ALTER TABLE orders DROP COLUMN IF EXISTS shipping_memo;

-- 4. Orders 테이블 인덱스 추가
CREATE INDEX idx_store_id ON orders (store_id);

-- =================================================================================
-- Payments 테이블 구조 변경
-- =================================================================================

-- 5. Payments 테이블에 새로운 컬럼들 추가
ALTER TABLE payments
ADD COLUMN store_id BIGINT NULL,
ADD COLUMN method VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
ADD COLUMN failed_at TIMESTAMP NULL,
ADD COLUMN canceled_at TIMESTAMP NULL;

-- 6. Payments 테이블 컬럼명 변경: payment_status → status
ALTER TABLE payments RENAME COLUMN payment_status TO status;
ALTER TABLE payments ALTER COLUMN status TYPE VARCHAR(20);

-- 7. Payments 테이블 컬럼명 변경: pg_tid → pg_transaction_id
ALTER TABLE payments RENAME COLUMN pg_tid TO pg_transaction_id;
ALTER TABLE payments ALTER COLUMN pg_transaction_id TYPE VARCHAR(100);


-- 8. Payments 테이블 인덱스 추가
CREATE INDEX idx_payments_store_id ON payments (store_id);
CREATE INDEX idx_payments_method ON payments (method);
CREATE INDEX idx_payments_status ON payments (status);

-- =================================================================================
-- 데이터 정리 및 기본값 설정 해제
-- =================================================================================

-- 9. 임시로 설정한 기본값들 제거 (실제 운영에서는 적절한 데이터로 업데이트 후 제거)
-- 개발/테스트 환경에서만 실행하고, 운영 환경에서는 실제 데이터 마이그레이션 후 진행
ALTER TABLE orders ALTER COLUMN store_id DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN recipient_name DROP DEFAULT;
ALTER TABLE payments ALTER COLUMN method DROP DEFAULT;

-- =================================================================================
-- 마이그레이션 완료 메시지
-- =================================================================================
-- Migration V2 completed: Orders and Payments tables updated successfully
