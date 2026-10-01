package com.miyuki.Inventory.Management.order.dto;

public record CreateOrderItemRequest(
        Long product_id,
        Long warehouse_id,
        Long quantity
) {
}
