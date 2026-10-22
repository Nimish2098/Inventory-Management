package com.miyuki.Inventory.Management.warehouses.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(

        @NotBlank
        @Size(max=50)
        String code,

        @NotBlank
        @Size(max=150)
        String name,

        @NotBlank
        @Min(1)
        Integer capacity
) {
}