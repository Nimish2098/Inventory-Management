package com.miyuki.Inventory.Management.warehouses;


import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService){
        this.warehouseService = warehouseService;
    }

    @PostMapping
    public  Warehouse createWarehouse(Warehouse warehouse){
        return warehouseService.createWareHouse(warehouse);
    }

    @GetMapping
    public List<Warehouse> getAllActiveWarehouse(){
        return warehouseService.getAllActiveWarehouse();
    }


    @PostMapping("/{id}")
    public Warehouse updateCapacity(@PathVariable Long id,@RequestBody Integer capacity){
        return warehouseService.updateCapacity(id,capacity);
    }

    @GetMapping("/{id}")
    public Warehouse getWarehouseDetails(@PathVariable Long id){
        return warehouseService.getWareHouseById(id);
    }
}
