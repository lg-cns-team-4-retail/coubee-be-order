-- V3: 결제 완료 시점 및 이벤트 타입 컬럼 추가
-- 작성일: 2025-08-13
-- 목적: 주문 결제 완료 시점 추적 및 주문 아이템 이벤트 타입 관리

-- =================================================================================
-- Orders 테이블에 결제 완료 시점 컬럼 추가
-- =================================================================================

-- 1. Orders 테이블에 paid_at_unix 컬럼 추가
-- QR 코드 스캔으로 주문이 결제 완료된 시점의 UNIX 타임스탬프를 저장
ALTER TABLE orders
ADD COLUMN paid_at_unix BIGINT NULL;

-- 2. paid_at_unix 컬럼에 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_orders_paid_at_unix ON orders (paid_at_unix);

-- =================================================================================
-- Order_items 테이블에 이벤트 타입 컬럼 추가
-- =================================================================================

-- 3. Order_items 테이블에 event_type 컬럼 추가
-- 주문 아이템의 이벤트 타입을 저장 (예: 'PURCHASE', 'REFUND' 등)
ALTER TABLE order_items 
ADD COLUMN event_type VARCHAR(50) NULL;

-- 4. event_type 컬럼에 인덱스 추가 (이벤트 타입별 조회 성능 향상)
CREATE INDEX idx_order_items_event_type ON order_items (event_type);

-- =================================================================================
-- 기존 데이터 업데이트
-- =================================================================================

-- 5. 결제 완료된 주문들의 paid_at_unix를 현재 시점으로 설정
-- status가 'PAID'인 모든 주문에 대해 현재 UNIX 타임스탬프를 설정
UPDATE orders
SET paid_at_unix = EXTRACT(EPOCH FROM NOW())::BIGINT
WHERE status = 'PAID'
  AND paid_at_unix IS NULL;

-- 6. 결제 완료된 주문의 모든 주문 아이템을 'PURCHASE' 이벤트로 설정
-- status가 'PAID'인 주문에 속한 모든 order_items의 event_type을 'PURCHASE'로 설정
UPDATE order_items 
SET event_type = 'PURCHASE'
WHERE order_id IN (
    SELECT id
    FROM orders 
    WHERE status = 'PAID'
) 
AND event_type IS NULL;

-- =================================================================================
-- 데이터 검증 쿼리 (주석으로 제공)
-- =================================================================================

-- 마이그레이션 후 데이터 검증을 위한 쿼리:
-- SELECT
--     o.user_id,
--     oi.product_id,
--     o.paid_at_unix,
--     oi.event_type,
--     o.status,
--     COUNT(*) as item_count
-- FROM
--     orders AS o
-- INNER JOIN
--     order_items AS oi ON o.order_id = oi.order_id
-- WHERE
--     o.status = 'PAID'
-- GROUP BY
--     o.user_id, oi.product_id, o.paid_at_unix, oi.event_type, o.status
-- ORDER BY
--     o.paid_at_unix DESC;

-- =================================================================================
-- 마이그레이션 완료
-- =================================================================================
-- Migration V3 completed:
-- - Orders 테이블에 paid_at_unix 컬럼 추가 완료
-- - Order_items 테이블에 event_type 컬럼 추가 완료
-- - 기존 PAID 상태 주문들의 데이터 업데이트 완료
