-- V10: 주문 테이블 스키마 정리 - 할인 컬럼 추가 및 사용하지 않는 토큰 컬럼 제거
-- 핫딜 할인 기능을 위한 originalAmount, discountAmount 컬럼 추가
-- 사용하지 않는 order_token, order_qr 컬럼 제거로 스키마 정리

-- =================================================================================
-- 1단계: Orders 테이블에 할인 관련 컬럼 추가
-- =================================================================================

-- 1-1. 원래 금액 (할인 전 총 금액) 컬럼 추가
ALTER TABLE orders ADD COLUMN original_amount INTEGER;

-- 1-2. 할인 금액 컬럼 추가 (기본값 0으로 설정)
ALTER TABLE orders ADD COLUMN discount_amount INTEGER NOT NULL DEFAULT 0;

-- =================================================================================
-- 2단계: 기존 데이터 마이그레이션
-- =================================================================================

-- 2-1. 기존 주문들의 original_amount를 total_amount로 설정
-- (기존 주문들은 할인이 없었으므로 original_amount = total_amount)
UPDATE orders SET original_amount = total_amount WHERE original_amount IS NULL;

-- 2-2. original_amount를 NOT NULL로 변경
ALTER TABLE orders ALTER COLUMN original_amount SET NOT NULL;

-- =================================================================================
-- 3단계: 사용하지 않는 컬럼 제거 (스키마 정리)
-- =================================================================================

-- 3-1. order_token 컬럼 제거 (더 이상 사용하지 않음)
ALTER TABLE orders DROP COLUMN IF EXISTS order_token;

-- 3-2. order_qr 컬럼 제거 (더 이상 사용하지 않음)
ALTER TABLE orders DROP COLUMN IF EXISTS order_qr;

-- =================================================================================
-- 4단계: 인덱스 추가 (성능 최적화)
-- =================================================================================

-- 4-1. 할인 관련 쿼리 성능을 위한 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_orders_discount_amount ON orders (discount_amount);
CREATE INDEX IF NOT EXISTS idx_orders_original_amount ON orders (original_amount);

-- =================================================================================
-- 마이그레이션 완료 메시지
-- =================================================================================
-- Migration V10 completed: 
-- - Added discount columns (original_amount, discount_amount) to orders table
-- - Removed unused token columns (order_token, order_qr) from orders table
-- - Schema refinement completed for better maintainability
