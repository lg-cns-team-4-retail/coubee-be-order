-- Create processed_webhooks table for webhook idempotency
CREATE TABLE processed_webhooks (
    webhook_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Add index on processed_at for potential cleanup queries
CREATE INDEX idx_processed_webhooks_processed_at ON processed_webhooks(processed_at);

-- Add comment for documentation
COMMENT ON TABLE processed_webhooks IS 'Stores processed webhook IDs to ensure idempotency in webhook handling';
COMMENT ON COLUMN processed_webhooks.webhook_id IS 'Unique webhook ID from the webhook provider';
COMMENT ON COLUMN processed_webhooks.processed_at IS 'Timestamp when the webhook was first processed';
