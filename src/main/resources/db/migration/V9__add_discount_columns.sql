-- V9: 주문 테이블에 할인 관련 컬럼 추가
-- 핫딜 할인 기능을 위한 originalAmount, discountAmount 컬럼 추가

-- =================================================================================
-- Orders 테이블에 할인 관련 컬럼 추가
-- =================================================================================

-- 1. 원래 금액 (할인 전 총 금액) 컬럼 추가
ALTER TABLE orders ADD COLUMN original_amount INTEGER;

-- 2. 할인 금액 컬럼 추가
ALTER TABLE orders ADD COLUMN discount_amount INTEGER NOT NULL DEFAULT 0;

-- =================================================================================
-- 기존 데이터 마이그레이션
-- =================================================================================

-- 3. 기존 주문들의 original_amount를 total_amount로 설정
-- (기존 주문들은 할인이 없었으므로 original_amount = total_amount)
UPDATE orders SET original_amount = total_amount WHERE original_amount IS NULL;

-- 4. original_amount를 NOT NULL로 변경
ALTER TABLE orders ALTER COLUMN original_amount SET NOT NULL;

-- =================================================================================
-- 인덱스 추가 (성능 최적화)
-- =================================================================================

-- 5. 할인 관련 쿼리 성능을 위한 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_orders_discount_amount ON orders (discount_amount);
CREATE INDEX IF NOT EXISTS idx_orders_original_amount ON orders (original_amount);

-- =================================================================================
-- 마이그레이션 완료 메시지
-- =================================================================================
-- Migration V9 completed: Added discount columns (original_amount, discount_amount) to orders table
