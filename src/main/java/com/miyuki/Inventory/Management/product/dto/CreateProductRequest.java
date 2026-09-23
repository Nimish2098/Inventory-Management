package com.miyuki.Inventory.Management.product.dto;

public record CreateProductRequest (
        String sku,
        String name,
        Long reorder_level
){}
