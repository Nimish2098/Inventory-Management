package com.miyuki.Inventory.Management.stock.dto;

public record InitialStockRequest(
    Long product_id,
    Long warehouse_id,
    Long quantity,
    String bin
) {}
