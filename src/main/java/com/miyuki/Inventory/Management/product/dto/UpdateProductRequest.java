package com.miyuki.Inventory.Management.product.dto;

public record UpdateProductRequest(
        Long id,
        String sku,
        String name,
        Long reorder_level
) {
}
