-- V11: Add was_hotdeal column to order_items table
-- Purpose: Track whether each order item was purchased during a store's active Hotdeal period
-- This resolves the SchemaManagementException: missing column [was_hotdeal] in table [order_items]

-- Add was_hotdeal column to order_items table
-- Default to false for existing records to maintain data integrity
ALTER TABLE order_items ADD COLUMN was_hotdeal BOOLEAN NOT NULL DEFAULT false;

-- Add index for performance when querying by hotdeal status
-- This will optimize dashboard queries that filter by hotdeal vs regular sales
CREATE INDEX IF NOT EXISTS idx_order_items_was_hotdeal ON order_items (was_hotdeal);

-- Add comment to document the column purpose for future developers
COMMENT ON COLUMN order_items.was_hotdeal IS 'Indicates whether this order item was purchased during an active store Hotdeal period. Set at order creation time based on real-time store hotdeal status.';
