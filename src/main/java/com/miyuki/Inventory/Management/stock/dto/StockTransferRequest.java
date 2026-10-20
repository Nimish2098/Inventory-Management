package com.miyuki.Inventory.Management.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockTransferRequest (
        @NotNull(message = "Product ID is required")
        Long product_id,

        @NotNull(message = "Source warehouse ID is required")
        Long from_warehouse_id,

        @NotNull(message = "Destination warehouse ID is required")
        Long to_warehouse_id,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than zero")
        Long quantity
)
{}
