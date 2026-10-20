package com.miyuki.Inventory.Management.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest (
        @NotBlank(message = "SKU is required")
        @Size(max = 255, message = "SKU must be at most 255 characters")
        String sku,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 255, message = "Category must be at most 255 characters")
        String category,

        @NotNull(message = "Reorder level is required")
        @Min(value = 0, message = "Reorder level must be zero or greater")
        Long reorder_level
){}
