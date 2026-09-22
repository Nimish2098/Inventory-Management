package com.miyuki.Inventory.Management.stock;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryitemController {

    private final InventoryitemService inventoryitemService;

    public InventoryitemController(InventoryitemService inventoryitemService){
        this.inventoryitemService = inventoryitemService;
    }

}
