package com.miyuki.Inventory.Management.warehouses.dto;

public record UpdateWarehouseRequest(
        String code,
        String name,
        Integer capacity,
        boolean isActive
) {
}
