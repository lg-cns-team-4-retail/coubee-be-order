-- V13: Refactor order amount calculation columns to support product-specific and promotional discounts
-- This migration restructures the orders table to correctly handle:
-- 1. totalOriginalAmount: sum of all items' originPrice (before any discounts)
-- 2. totalDiscountAmount: sum of ALL discounts (product-specific + hotdeal)
-- 3. finalPurchaseAmount: final amount paid after all discounts

-- Step 1: Rename total_amount to final_payment_amount (temporary)
-- The current total_amount holds the final payment amount, so we preserve it temporarily
ALTER TABLE orders RENAME COLUMN total_amount TO final_payment_amount;

-- Step 2: Rename original_amount to total_amount
-- The current original_amount actually holds the sum of salePrice, so we rename it to total_amount
ALTER TABLE orders RENAME COLUMN original_amount TO total_amount;

-- Step 3: Add new original_amount column
-- This will store the sum of originPrice for all items
ALTER TABLE orders ADD COLUMN original_amount INTEGER NOT NULL DEFAULT 0;

-- Step 4: Migrate data - populate the new original_amount column
-- For existing records, assume originPrice was equal to salePrice (no product-specific discounts)
-- So we copy the values from total_amount (which now holds the salePrice sum)
UPDATE orders SET original_amount = total_amount;

-- Step 5: Ensure data consistency for final_payment_amount
-- Recalculate final_payment_amount as total_amount (salePrice sum) minus discount_amount (hotdeal discount)
UPDATE orders SET final_payment_amount = total_amount - discount_amount;

-- Step 6: Drop the old total_amount column and rename final_payment_amount back to total_amount
ALTER TABLE orders DROP COLUMN total_amount;
ALTER TABLE orders RENAME COLUMN final_payment_amount TO total_amount;

-- Final schema after migration:
-- original_amount: sum of all items' originPrice (before any discounts)
-- discount_amount: sum of ALL discounts applied (product-specific + hotdeal)
-- total_amount: final amount paid by customer after all discounts
