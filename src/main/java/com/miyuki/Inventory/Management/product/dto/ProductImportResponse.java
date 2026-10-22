package com.miyuki.Inventory.Management.product.dto;

import java.util.List;

public record ProductImportResponse(
    int totalRows,
    int imported,
    int failed,
    List<RowError> errors
) {
  public record RowError(
      int row,
      String sku,
      String message
  ) {}
}