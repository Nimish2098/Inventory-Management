package com.miyuki.Inventory.Management.stock.dto;

import com.miyuki.Inventory.Management.stock.StockMovementType;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest (
        @NotNull(message = "Product ID is required")
        Long product_id,

        @NotNull(message = "Warehouse ID is required")
        Long warehouse_id,

        @NotNull(message = "Quantity is required")
        Long quantity,

        @NotNull(message = "Movement type is required")
        StockMovementType movementType
)
{}
