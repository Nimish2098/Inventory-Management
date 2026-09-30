package com.miyuki.Inventory.Management.warehouses.dto;

public record WarehouseResponse(

        Long id,
        String code,
        String name,
        Integer capacity,
        boolean isActive
) {
}
