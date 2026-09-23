package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.stock.dto.StockAdjustmentRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryitemController {

    private final InventoryitemService inventoryitemService;

    public InventoryitemController(InventoryitemService inventoryitemService){
        this.inventoryitemService = inventoryitemService;
    }


    @PostMapping
    public String adjustStock(StockAdjustmentRequest request){

        inventoryitemService.adjustStock(request);
        return "Stock Updated Successfully";
    }
}
