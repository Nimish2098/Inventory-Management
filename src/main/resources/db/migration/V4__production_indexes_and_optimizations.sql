-- Indexes for frequently queried fields and joins
CREATE INDEX IF NOT EXISTS idx_product_owner ON product (owner_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_owner ON warehouse (owner_id);
CREATE INDEX IF NOT EXISTS idx_customer_owner ON customer (owner_id);
CREATE INDEX IF NOT EXISTS idx_inventory_item_product ON inventory_item (product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_item_warehouse ON inventory_item (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_item ON stock_movement (inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_customer_order_customer ON customer_order (customer_id);
CREATE INDEX IF NOT EXISTS idx_order_item_order ON order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product ON order_item (product_id);
CREATE INDEX IF NOT EXISTS idx_order_item_warehouse ON order_item (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_product_watch_owner ON product_watch (owner_id);
CREATE INDEX IF NOT EXISTS idx_product_watch_enabled_id ON product_watch (enabled, id);
CREATE INDEX IF NOT EXISTS idx_product_observation_watch_time ON product_observation (watch_id, observed_at);
