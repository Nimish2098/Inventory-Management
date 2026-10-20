CREATE TABLE IF NOT EXISTS user_account (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS customer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    owner_id BIGINT REFERENCES user_account(id),
    CONSTRAINT uk_customer_owner_email UNIQUE (owner_id, email)
);

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255),
    name VARCHAR(255),
    category VARCHAR(255),
    reorder_level BIGINT,
    owner_id BIGINT REFERENCES user_account(id),
    CONSTRAINT uk_product_owner_sku UNIQUE (owner_id, sku)
);

CREATE TABLE IF NOT EXISTS warehouse (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255),
    name VARCHAR(255),
    capacity INTEGER,
    active BOOLEAN NOT NULL DEFAULT true,
    owner_id BIGINT REFERENCES user_account(id),
    CONSTRAINT uk_warehouse_owner_code UNIQUE (owner_id, code)
);

CREATE TABLE IF NOT EXISTS inventory_item (
    id BIGSERIAL PRIMARY KEY,
    bin VARCHAR(255),
    quantity BIGINT,
    product_id BIGINT NOT NULL REFERENCES product(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouse(id),
    CONSTRAINT uk_inventory_item_product_warehouse UNIQUE (product_id, warehouse_id)
);

CREATE TABLE IF NOT EXISTS stock_movement (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_item(id),
    quantity_change BIGINT,
    type VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS customer_order (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES customer_order(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouse(id),
    quantity BIGINT NOT NULL
);
