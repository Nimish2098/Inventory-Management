package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.stock.dto.InventoryItemResponse;
import com.miyuki.Inventory.Management.stock.dto.StockMovementResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryItemResponse toResponse(InventoryItem item) {
        Long reorderLevel = item.getProduct().getReorderLevel();
        boolean lowStock = reorderLevel != null && item.getQuantity() < reorderLevel;
        return new InventoryItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getWarehouse().getId(),
                item.getWarehouse().getCode(),
                item.getQuantity(),
                item.getBin(),
                reorderLevel,
                lowStock
        );
    }

    public StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getInventoryItem().getProduct().getId(),
                movement.getInventoryItem().getWarehouse().getId(),
                movement.getQuantityChange(),
                movement.getType()
        );
    }
}
