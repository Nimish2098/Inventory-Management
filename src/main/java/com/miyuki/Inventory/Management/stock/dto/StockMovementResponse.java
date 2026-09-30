package com.miyuki.Inventory.Management.stock.dto;

import com.miyuki.Inventory.Management.stock.StockMovementType;

public record StockMovementResponse(
        Long id,
        Long product_id,
        Long warehouse_id,
        Long quantity_change,
        StockMovementType type
) {
}
