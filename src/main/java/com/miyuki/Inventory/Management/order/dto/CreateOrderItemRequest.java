package com.miyuki.Inventory.Management.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotNull(message = "Product ID is required")
        Long product_id,

        @NotNull(message = "Warehouse ID is required")
        Long warehouse_id,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than zero")
        Long quantity
) {
}
