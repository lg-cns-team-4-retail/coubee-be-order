-- V6: Create order_timestamp table for tracking order status changes
-- 작성일: 2025-08-26
-- 목적: 주문 상태 변경 이력을 추적하기 위한 테이블 생성

-- =================================================================================
-- Order_timestamp 테이블 생성
-- =================================================================================

-- 1. order_timestamp 테이블 생성
-- 주문의 모든 상태 변경 이력을 시간순으로 기록합니다.
CREATE TABLE order_timestamp (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

-- 2. 성능 최적화를 위한 인덱스 생성
-- order_id로 특정 주문의 이력을 빠르게 조회하기 위한 인덱스
CREATE INDEX idx_order_timestamp_order_id ON order_timestamp (order_id);

-- updated_at으로 시간순 정렬 조회를 위한 인덱스
CREATE INDEX idx_order_timestamp_updated_at ON order_timestamp (updated_at);

-- order_id와 updated_at 복합 인덱스 (가장 자주 사용될 조회 패턴)
CREATE INDEX idx_order_timestamp_order_id_updated_at ON order_timestamp (order_id, updated_at);

-- =================================================================================
-- 기존 주문 데이터에 대한 초기 이력 생성
-- =================================================================================

-- 3. 기존 주문들에 대해 현재 상태를 기준으로 초기 이력 레코드 생성
-- 각 주문의 현재 상태를 created_at 시점의 이력으로 기록합니다.
INSERT INTO order_timestamp (order_id, status, updated_at)
SELECT 
    o.order_id,
    o.status,
    o.created_at
FROM orders o
WHERE NOT EXISTS (
    SELECT 1 FROM order_timestamp ot 
    WHERE ot.order_id = o.order_id
);

-- =================================================================================
-- 데이터 검증 쿼리 (주석으로 제공)
-- =================================================================================

-- 마이그레이션 후 데이터 검증을 위한 쿼리:
-- 
-- 1. 각 주문별 이력 개수 확인:
-- SELECT 
--     o.order_id,
--     o.status as current_status,
--     COUNT(ot.id) as history_count,
--     MIN(ot.updated_at) as first_status_change,
--     MAX(ot.updated_at) as last_status_change
-- FROM orders o
-- LEFT JOIN order_timestamp ot ON o.order_id = ot.order_id
-- GROUP BY o.order_id, o.status
-- ORDER BY o.created_at DESC;
--
-- 2. 특정 주문의 상태 변경 이력 조회:
-- SELECT 
--     ot.status,
--     ot.updated_at,
--     LAG(ot.status) OVER (ORDER BY ot.updated_at) as previous_status
-- FROM order_timestamp ot
-- WHERE ot.order_id = 'your_order_id_here'
-- ORDER BY ot.updated_at ASC;
--
-- 3. 상태별 주문 수 통계:
-- SELECT 
--     ot.status,
--     COUNT(DISTINCT ot.order_id) as order_count,
--     COUNT(ot.id) as total_status_changes
-- FROM order_timestamp ot
-- GROUP BY ot.status
-- ORDER BY order_count DESC;

-- =================================================================================
-- 마이그레이션 완료
-- =================================================================================
-- Migration V6 completed:
-- - order_timestamp 테이블 생성 완료
-- - 성능 최적화 인덱스 생성 완료  
-- - 기존 주문 데이터에 대한 초기 이력 생성 완료
-- - 외래 키 제약 조건으로 데이터 무결성 보장
-- =================================================================================
