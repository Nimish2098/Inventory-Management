package com.miyuki.Inventory.Management.stock.dto;

public record StockAdjustmentRequest (

        Long product_id,
        Long warehouse_id,
        Long quantity,
        Enum movementType
)
{}
