-- 주문 테이블
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    order_status VARCHAR(20) NOT NULL,
    shipping_address VARCHAR(255),
    shipping_memo VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id)
);

-- 주문 상품 테이블
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    price BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id)
);

-- 결제 테이블
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    pg_provider VARCHAR(50),
    pg_tid VARCHAR(100),
    paid_at DATETIME,
    fail_reason VARCHAR(255),
    cancel_reason VARCHAR(255),
    receipt_url VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_payment_id (payment_id)
); 