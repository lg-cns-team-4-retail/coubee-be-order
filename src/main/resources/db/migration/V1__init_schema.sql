-- 주문 테이블
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    order_status VARCHAR(20) NOT NULL,
    shipping_address VARCHAR(255),
    shipping_memo VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_user_id ON orders (user_id);
CREATE INDEX idx_order_id ON orders (order_id);

-- 주문 상품 테이블
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    price BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
CREATE INDEX idx_product_id ON order_items (product_id);

-- 결제 테이블
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    pg_provider VARCHAR(50),
    pg_tid VARCHAR(100),
    paid_at TIMESTAMP,
    fail_reason VARCHAR(255),
    cancel_reason VARCHAR(255),
    receipt_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
CREATE INDEX idx_payment_id ON payments (payment_id);
 