package com.miyuki.Inventory.Management.product.dto;

public record UpdateProductRequest(
        String sku,
        String name,
        String category,
        Long reorder_level
) {
}
