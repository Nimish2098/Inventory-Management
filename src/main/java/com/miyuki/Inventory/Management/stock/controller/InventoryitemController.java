package com.miyuki.Inventory.Management.stock.controller;

import com.miyuki.Inventory.Management.stock.dto.InitialStockRequest;
import com.miyuki.Inventory.Management.stock.dto.InventoryInitialImportResponse;
import com.miyuki.Inventory.Management.stock.dto.StockAdjustmentRequest;
import com.miyuki.Inventory.Management.stock.dto.StockTransferRequest;
import com.miyuki.Inventory.Management.stock.dto.InventoryItemResponse;
import com.miyuki.Inventory.Management.stock.dto.StockMovementResponse;
import com.miyuki.Inventory.Management.stock.service.InventoryInitialItemCsvImportService;
import com.miyuki.Inventory.Management.stock.service.InventoryitemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryitemController {

    private final InventoryitemService inventoryitemService;
    private final InventoryInitialItemCsvImportService csvImportService;
    public InventoryitemController(InventoryitemService inventoryitemService, InventoryInitialItemCsvImportService importService){
        this.inventoryitemService = inventoryitemService;
        this.csvImportService = importService;
    }

    @PostMapping("/initial-stock")
    public InventoryItemResponse initializeStock(
        @RequestBody InitialStockRequest request) {
        return inventoryitemService.initializeStock(request);
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
    @PostMapping(
        value = "/initial-stock/import",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<InventoryInitialImportResponse> importInitialStock(
        @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(
            csvImportService.importCsv(file)
        );
    }
}