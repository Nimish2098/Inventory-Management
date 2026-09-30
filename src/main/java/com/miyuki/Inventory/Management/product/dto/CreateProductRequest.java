package com.miyuki.Inventory.Management.product.dto;

public record CreateProductRequest (
        String sku,
        String name,
        String category,
        Long reorder_level
){}
