package com.miyuki.Inventory.Management.warehouses.dto;

public record CreateWarehouseRequest(

        String code,
        String name,
        Integer capacity
) {
}
