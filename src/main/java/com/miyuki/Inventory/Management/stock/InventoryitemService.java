package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.dto.StockAdjustmentRequest;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryitemService {

        private final InventoryitemRepository inventoryitemRepository;
        private final StockMovementRepository stockMovementRepository;

        public InventoryitemService(InventoryitemRepository inventoryitemRepository,StockMovementRepository stockMovementRepository){
            this.inventoryitemRepository = inventoryitemRepository;
            this.stockMovementRepository = stockMovementRepository;
        }

        public void adjustStock(StockAdjustmentRequest request){


        }

}
