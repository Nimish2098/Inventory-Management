package com.miyuki.Inventory.Management.stock.dto;

import java.util.List;

public record InventoryInitialImportResponse(
    int totalRows,
    int imported,
    int failed,
    List<RowError> errors
) {
  public record RowError(
      int row,
      String productSku,
      String warehouseCode,
      String message
  ) {}
}
