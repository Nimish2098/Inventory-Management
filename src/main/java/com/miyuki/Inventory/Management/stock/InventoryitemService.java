package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.common.exception.InsufficientStockException;
import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.dto.StockAdjustmentRequest;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.stereotype.Service;

import javax.naming.InsufficientResourcesException;
import java.util.Optional;

@Service
public class InventoryitemService {

        private final InventoryitemRepository inventoryitemRepository;
        private final StockMovementRepository stockMovementRepository;

        public InventoryitemService(InventoryitemRepository inventoryitemRepository,StockMovementRepository stockMovementRepository){
            this.inventoryitemRepository = inventoryitemRepository;
            this.stockMovementRepository = stockMovementRepository;
        }

        public void adjustStock(StockAdjustmentRequest request){

            InventoryItem item = inventoryitemRepository.findByProductIdAndWarehouseId(request.product_id(), request.warehouse_id())
                    .orElseThrow(()-> new ResourceNotFoundException("Inventory location not found"));

            Long newQuantity = item.getQuantity()+ request.quantity();
            if(newQuantity <0){
                throw new InsufficientStockException("Stock level cannot drop below 0");
            }
            item.setQuantity(newQuantity);
            inventoryitemRepository.save(item);

            StockMovement stockMovement = StockMovement.builder()
                    .inventoryItem(item)
                    .quantity_change(request.quantity())
                    .type(request.movementType())
                    .build();

            stockMovementRepository.save(stockMovement);

        }

}
