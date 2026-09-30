package com.miyuki.Inventory.Management.warehouses;

import com.miyuki.Inventory.Management.warehouses.dto.CreateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.UpdateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.WarehouseResponse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public Warehouse toEntity(CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setCapacity(request.capacity());
        return warehouse;
    }

    public void updateEntity(UpdateWarehouseRequest request, Warehouse warehouse) {
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setCapacity(request.capacity());
        warehouse.setActive(request.isActive());
    }

    public WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getCapacity(),
                warehouse.isActive()
        );
    }
}
