package com.miyuki.Inventory.Management.stock.dto;

public record StockUpdateResponse(

        Long product_id,
        Long quantity
) {
}
