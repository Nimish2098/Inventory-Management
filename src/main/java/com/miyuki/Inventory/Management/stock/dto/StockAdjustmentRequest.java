package com.miyuki.Inventory.Management.stock.dto;

import com.miyuki.Inventory.Management.stock.model.StockMovementType;

public record StockAdjustmentRequest (

        Long product_id,
        Long warehouse_id,
        Long quantity,
        StockMovementType movementType
)
{}