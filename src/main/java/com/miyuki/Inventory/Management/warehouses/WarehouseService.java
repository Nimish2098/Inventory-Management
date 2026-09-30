package com.miyuki.Inventory.Management.warehouses;

import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.warehouses.dto.CreateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.UpdateWarehouseRequest;
import com.miyuki.Inventory.Management.warehouses.dto.WarehouseResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {


    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseRepository repository, WarehouseMapper warehouseMapper){
        this.warehouseRepository =repository;
        this.warehouseMapper = warehouseMapper;
    }

    public WarehouseResponse createWarehouse(CreateWarehouseRequest request){
        validate(request.code(), request.name(), request.capacity());
        if(warehouseRepository.existsByCode(request.code())){
            throw new IllegalStateException("Warehouse code already exists");
        }
        Warehouse warehouse = warehouseMapper.toEntity(request);
        warehouse.setActive(true);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public WarehouseResponse getWarehouseById(Long warehouseId){
        return warehouseMapper.toResponse(getEntity(warehouseId));
    }

    public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request){
        validate(request.code(), request.name(), request.capacity());
        Warehouse warehouse = getEntity(id);
        warehouseRepository.findByCode(request.code()).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalStateException("Warehouse code already exists"); });
        warehouseMapper.updateEntity(request, warehouse);
        return warehouseMapper.toResponse(warehouseRepository.save(warehouse));
    }

    public List<WarehouseResponse> getAllActiveWarehouses(){
        return warehouseRepository.findByActiveTrue().stream().map(warehouseMapper::toResponse).toList();
    }

    public void deleteWarehouse(Long id) {
        warehouseRepository.delete(getEntity(id));
    }

    private Warehouse getEntity(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse " + id + " not found"));
    }

    private void validate(String code, String name, Integer capacity) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Warehouse code and name are required");
        }
        if (capacity == null || capacity < 0) {
            throw new IllegalArgumentException("Capacity must be zero or greater");
        }
    }

}
