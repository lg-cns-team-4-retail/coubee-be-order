-- V4: order_items 테이블의 order_id 관계를 올바르게 수정
-- 목적: order_items.order_id 컬럼을 orders.id(BIGINT)가 아닌 orders.order_id(VARCHAR)를 참조하도록 변경합니다.

-- 1. 기존의 잘못된 외래 키 제약 조건 삭제
-- 이 제약 조건은 order_items.order_id(BIGINT)가 orders.id(BIGINT)를 참조하고 있습니다.
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;


-- 2. 임시로 사용할 컬럼을 추가하여 기존의 숫자 ID(orders.id 값)를 백업합니다.
-- 데이터 유실을 방지하기 위한 중요한 단계입니다.
ALTER TABLE order_items ADD COLUMN temp_order_pk_id BIGINT;
UPDATE order_items SET temp_order_pk_id = order_id;


-- 3. 기존 order_id 컬럼의 타입을 VARCHAR(50)으로 변경합니다.
-- orders.order_id 컬럼의 타입과 일치시키기 위함입니다.
-- USING NULL을 통해 타입 변경 중 발생할 수 있는 데이터 변환 오류를 방지합니다.
ALTER TABLE order_items ALTER COLUMN order_id TYPE VARCHAR(50) USING NULL;


-- 4. 백업해 둔 숫자 ID를 이용해 orders 테이블과 조인하여 올바른 문자열 order_id를 찾아 채워 넣습니다.
UPDATE order_items oi
SET order_id = o.order_id
FROM orders o
WHERE oi.temp_order_pk_id = o.id;


-- 5. 데이터 업데이트가 완료되었으므로 임시 컬럼을 삭제합니다.
ALTER TABLE order_items DROP COLUMN temp_order_pk_id;


-- 6. order_id 컬럼에 NOT NULL 제약 조건을 다시 추가합니다.
ALTER TABLE order_items ALTER COLUMN order_id SET NOT NULL;


-- 7. 올바른 외래 키 제약 조건을 다시 설정합니다.
-- 이제 order_items.order_id(VARCHAR)가 orders.order_id(VARCHAR)를 올바르게 참조하게 됩니다.
ALTER TABLE order_items
ADD CONSTRAINT fk_order_items_to_orders_order_id
FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE;


-- 8. 성능을 위해 새로운 외래 키 컬럼에 인덱스를 생성합니다.
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);