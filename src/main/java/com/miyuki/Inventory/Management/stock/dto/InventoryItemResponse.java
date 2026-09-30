package com.miyuki.Inventory.Management.stock.dto;

public record InventoryItemResponse(
	Long id,
	Long product_id,
	String product_sku,
	Long warehouse_id,
	String warehouse_code,
	Long quantity,
	String bin,
	Long reorder_level,
	boolean low_stock
) {
}
