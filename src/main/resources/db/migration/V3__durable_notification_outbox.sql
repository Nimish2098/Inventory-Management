-- Manual PostgreSQL upgrade from the Phase 2 notification-event schema.
-- Run once before deploying the Phase 3 application.
ALTER TABLE product_notification_event
    ADD COLUMN IF NOT EXISTS event_key varchar(300),
    ADD COLUMN IF NOT EXISTS inventory_item_id bigint,
    ADD COLUMN IF NOT EXISTS warehouse_code varchar(100),
    ADD COLUMN IF NOT EXISTS current_quantity bigint,
    ADD COLUMN IF NOT EXISTS reorder_level bigint,
    ADD COLUMN IF NOT EXISTS status varchar(20),
    ADD COLUMN IF NOT EXISTS attempt_count integer,
    ADD COLUMN IF NOT EXISTS next_attempt_at timestamptz,
    ADD COLUMN IF NOT EXISTS last_attempt_at timestamptz,
    ADD COLUMN IF NOT EXISTS last_error_summary varchar(500);

UPDATE product_notification_event
SET event_key = 'legacy:' || id
WHERE event_key IS NULL;

UPDATE product_notification_event
SET status = CASE WHEN delivered_at IS NULL THEN 'PENDING' ELSE 'SENT' END
WHERE status IS NULL;

UPDATE product_notification_event
SET attempt_count = 0
WHERE attempt_count IS NULL;

UPDATE product_notification_event
SET next_attempt_at = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE next_attempt_at IS NULL;

UPDATE product_notification_event
SET last_attempt_at = delivered_at
WHERE last_attempt_at IS NULL
  AND delivered_at IS NOT NULL;

ALTER TABLE product_notification_event
    ALTER COLUMN event_key SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN attempt_count SET NOT NULL,
    ALTER COLUMN next_attempt_at SET NOT NULL,
    ALTER COLUMN watch_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_notification_event_key
    ON product_notification_event (event_key);

DROP INDEX IF EXISTS idx_product_notification_pending;
CREATE INDEX IF NOT EXISTS idx_product_notification_pending
    ON product_notification_event (status, next_attempt_at, created_at);
CREATE INDEX IF NOT EXISTS idx_product_notification_inventory
    ON product_notification_event (inventory_item_id, created_at);

ALTER TABLE inventory_alert_state
    ADD COLUMN IF NOT EXISTS episode_number bigint;

UPDATE inventory_alert_state
SET episode_number = CASE WHEN low_stock THEN 1 ELSE 0 END
WHERE episode_number IS NULL;
