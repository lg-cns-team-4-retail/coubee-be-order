-- V14: Add denormalized columns for enhanced order search
-- Adds store_name to orders and description to order_items to allow filtering
-- without requiring cross-service joins, significantly improving performance.

-- Step 1: Add store_name to the orders table
ALTER TABLE coubee_order.orders ADD COLUMN store_name VARCHAR(255);
COMMENT ON COLUMN coubee_order.orders.store_name IS 'Denormalized store name at the time of order for search optimization';

-- Step 2: Add description to the order_items table
ALTER TABLE coubee_order.order_items ADD COLUMN description TEXT;
COMMENT ON COLUMN coubee_order.order_items.description IS 'Denormalized product description at the time of order for search optimization';

-- Note: Populating these new columns for historical data is an offline task
-- and is not included in this migration script. New orders will have these fields populated.
