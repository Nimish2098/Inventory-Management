package com.miyuki.Inventory.Management.order.dto;

public record OrderItemResponse(
        Long id,
        Long product_id,
        Long warehouse_id,
        Long quantity
) {
}
