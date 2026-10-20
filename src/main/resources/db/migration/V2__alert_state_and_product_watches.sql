CREATE TABLE IF NOT EXISTS inventory_alert_state (
    inventory_item_id BIGINT PRIMARY KEY REFERENCES inventory_item(id),
    low_stock BOOLEAN NOT NULL,
    notification_sent BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS product_watch (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES user_account(id),
    product_name VARCHAR(200) NOT NULL,
    product_url VARCHAR(2048) NOT NULL,
    target_price NUMERIC(19, 4),
    latest_observed_price NUMERIC(19, 4),
    latest_stock_status VARCHAR(20),
    last_successful_check TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product_observation (
    id BIGSERIAL PRIMARY KEY,
    watch_id BIGINT NOT NULL REFERENCES product_watch(id) ON DELETE CASCADE,
    observed_price NUMERIC(19, 4) NOT NULL,
    stock_status VARCHAR(20) NOT NULL,
    target_price_at_observation NUMERIC(19, 4),
    observed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS product_notification_event (
    id BIGSERIAL PRIMARY KEY,
    watch_id BIGINT REFERENCES product_watch(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    previous_price NUMERIC(19, 4),
    current_price NUMERIC(19, 4),
    target_price NUMERIC(19, 4),
    product_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ
);
