package com.miyuki.Inventory.Management.warehouses.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 255, message = "Code must be at most 255 characters")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotNull(message = "Capacity is required")
        @Min(value = 0, message = "Capacity must be zero or greater")
        Integer capacity
) {
}
