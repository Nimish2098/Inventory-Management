package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.stock.dto.StockAdjustmentRequest;
import com.miyuki.Inventory.Management.stock.dto.StockTransferRequest;
import com.miyuki.Inventory.Management.stock.dto.InventoryItemResponse;
import com.miyuki.Inventory.Management.stock.dto.StockMovementResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryitemController {

    private final InventoryitemService inventoryitemService;

    public InventoryitemController(InventoryitemService inventoryitemService){
        this.inventoryitemService = inventoryitemService;
    }


    @PostMapping({"", "/adjustments"})
    public InventoryItemResponse adjustStock(@RequestBody StockAdjustmentRequest request){
        return inventoryitemService.adjustStock(request);
    }

    @PostMapping("/transfers")
    public List<InventoryItemResponse> transferStock(@RequestBody StockTransferRequest request) {
        return inventoryitemService.transferStock(request);
    }

    @GetMapping
    public List<InventoryItemResponse> getInventory(@RequestParam(required = false) Long warehouse_id) {
        return inventoryitemService.getInventory(warehouse_id);
    }

    @GetMapping("/low-stock")
    public List<InventoryItemResponse> getLowStockAlerts() {
        return inventoryitemService.getLowStockAlerts();
    }

    @GetMapping("/{productId}/{warehouseId}")
    public InventoryItemResponse getStockLevel(@PathVariable Long productId, @PathVariable Long warehouseId) {
        return inventoryitemService.getStockLevel(productId, warehouseId);
    }

    @GetMapping("/{productId}/{warehouseId}/movements")
    public List<StockMovementResponse> getMovementHistory(@PathVariable Long productId,
                                                           @PathVariable Long warehouseId) {
        return inventoryitemService.getMovementHistory(productId, warehouseId);
    }
}
