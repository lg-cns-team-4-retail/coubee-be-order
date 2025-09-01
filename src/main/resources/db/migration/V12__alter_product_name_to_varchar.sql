-- V12: Explicitly alter the product_name column type to VARCHAR to resolve the persistent type mismatch error.
ALTER TABLE order_items
ALTER COLUMN product_name TYPE VARCHAR(255);
