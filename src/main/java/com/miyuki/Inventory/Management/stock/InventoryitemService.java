package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryitemService {

    private final InventoryitemRepository inventoryitemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryitemService(InventoryitemRepository inventoryitemRepository,ProductRepository productRepository,WarehouseRepository warehouseRepository){
         this.inventoryitemRepository = inventoryitemRepository;
         this.productRepository = productRepository;
         this.warehouseRepository = warehouseRepository;
    }

    public InventoryItem createInventoryItem(InventoryItem inventoryItem){
        return inventoryitemRepository.save(inventoryItem);
    }

    public InventoryItem updateCurrentBalance(Long product_id,Long warehouse_id,Integer quantity){
        if(product_id==null || warehouse_id==null || quantity<0){
            throw new IllegalArgumentException("Product Id,Customer Id and Quantity is Required");
        }

        productRepository.findById(product_id).orElseThrow(
                () -> new IllegalArgumentException("Product Not Found")
        );

        warehouseRepository.findById(warehouse_id).orElseThrow(
                () -> new IllegalArgumentException("WareHouse not Found")
        );

        InventoryItem item = inventoryitemRepository.findByProductId(product_id).orElseThrow();
        item.setQuantity(quantity+item.getQuantity());
        return item;
    }


}
