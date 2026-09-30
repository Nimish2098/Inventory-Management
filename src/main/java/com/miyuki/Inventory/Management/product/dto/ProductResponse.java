package com.miyuki.Inventory.Management.product.dto;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String category,
    Long reorder_level
) {
}
