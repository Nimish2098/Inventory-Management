package com.miyuki.Inventory.Management.warehouses;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {


    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository repository){
        this.warehouseRepository =repository;
    }

    public Warehouse createWareHouse(Warehouse warehouse){
        if(warehouseRepository.existsById(warehouse.getId())){
            throw new RuntimeException("Warehouse already exists");
        }
        return warehouseRepository.save(warehouse);
    }

    public Warehouse getWareHouseById(Long wareHouseId){

        Warehouse warehouse = warehouseRepository.findById(wareHouseId).orElseThrow(

                () -> new RuntimeException("WareHouse Doesnt exist")
        );
        return warehouse;
    }

    public Warehouse updateCapacity(Long id,Integer newCapacity){
        if(!warehouseRepository.existsById(id)){
            throw new RuntimeException("Warehouse not exist");
        }

        Warehouse warehouse = warehouseRepository.findById(id).orElseThrow(

                () -> new RuntimeException("WareHouse Doesnt exist")
        );

        warehouse.setCapacity(warehouse.getCapacity()+newCapacity);
        return warehouse;
    }

    public List<Warehouse> getAllActiveWarehouse(){

        return warehouseRepository.findByActiveTrue();
    }

}
