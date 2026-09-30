package com.miyuki.Inventory.Management.stock.dto;

public record StockTransferRequest(
        Long product_id,
        Long from_warehouse_id,
        Long to_warehouse_id,
        Long quantity
)
{}
