package com.miyuki.Inventory.Management.warehouses;


import org.springframework.web.bind.annotation.*;
import com.miyuki.Inventory.Management.warehouses.dto.CreateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.UpdateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.WarehouseResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService){
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse createWarehouse(@RequestBody CreateWarehouseRequest request){
        return warehouseService.createWarehouse(request);
    }

    @GetMapping
    public List<WarehouseResponse> getAllActiveWarehouse(){
        return warehouseService.getAllActiveWarehouses();
    }

    @PutMapping("/{id}")
    public WarehouseResponse updateWarehouse(@PathVariable Long id,@RequestBody UpdateWarehouseRequest request){
        return warehouseService.updateWarehouse(id,request);
    }

    @GetMapping("/{id}")
    public WarehouseResponse getWarehouseDetails(@PathVariable Long id){
        return warehouseService.getWarehouseById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
    }
}
