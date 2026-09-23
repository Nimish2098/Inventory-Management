package com.miyuki.Inventory.Management.product.dto;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    Long reorder_level,
    boolean isLowStock
) {
}
