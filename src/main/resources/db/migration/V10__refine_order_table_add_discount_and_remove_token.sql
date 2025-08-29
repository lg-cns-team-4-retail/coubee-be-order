-- V10: 주문 테이블 스키마 정리 - 사용하지 않는 토큰 컬럼 제거
-- 사용하지 않는 order_token, order_qr 컬럼 제거로 스키마 정리
-- 참고: 할인 관련 컬럼(original_amount, discount_amount)은 V9에서 이미 추가됨

-- =================================================================================
-- 사용하지 않는 컬럼 제거 (스키마 정리)
-- =================================================================================

-- 1. order_token 컬럼 제거 (더 이상 사용하지 않음)
ALTER TABLE orders DROP COLUMN IF EXISTS order_token;

-- 2. order_qr 컬럼 제거 (더 이상 사용하지 않음)
ALTER TABLE orders DROP COLUMN IF EXISTS order_qr;

-- =================================================================================
-- 마이그레이션 완료 메시지
-- =================================================================================
-- Migration V10 completed:
-- - Removed unused token columns (order_token, order_qr) from orders table
-- - Schema cleanup completed for better maintainability
-- - Note: Discount columns were already added in V9 migration
