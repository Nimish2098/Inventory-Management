package com.miyuki.Inventory.Management.stock.service;

import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.dto.InitialStockRequest;
import com.miyuki.Inventory.Management.stock.dto.InventoryInitialImportResponse;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InventoryInitialItemCsvImportService {

  private static final Set<String> REQUIRED_HEADERS = Set.of(
      "product_sku",
      "warehouse_code",
      "quantity",
      "bin"
  );

  private final InventoryitemService inventoryitemService;
  private final ProductRepository productRepository;
  private final WarehouseRepository warehouseRepository;
  private final CurrentUser currentUser;
  public InventoryInitialItemCsvImportService(
      InventoryitemService inventoryitemService,
      ProductRepository productRepository,
      WarehouseRepository warehouseRepository,
      CurrentUser currentUser
  ) {
    this.inventoryitemService = inventoryitemService;
    this.productRepository = productRepository;
    this.warehouseRepository = warehouseRepository;
    this.currentUser = currentUser;
  }

  public InventoryInitialImportResponse importCsv(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Please upload a non-empty CSV file.");
    }

    if (file.getOriginalFilename() == null
        || !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
      throw new IllegalArgumentException("Only CSV files are supported.");
    }

    List<InventoryInitialImportResponse.RowError> errors =
        new ArrayList<>();

    int totalRows = 0;
    int imported = 0;

    try (Reader reader = new InputStreamReader(
        file.getInputStream(), StandardCharsets.UTF_8);
         CSVParser parser = CSVFormat.DEFAULT.builder()
                                             .setHeader()
                                             .setSkipHeaderRecord(true)
                                             .setIgnoreEmptyLines(true)
                                             .setTrim(true)
                                             .get()
                                             .parse(reader)) {

      Map<String, Integer> headers = parser.getHeaderMap();

      if (headers == null
          || !headers.keySet().containsAll(REQUIRED_HEADERS)) {
        throw new IllegalArgumentException(
            "CSV must contain these headers: "
            + String.join(", ", REQUIRED_HEADERS)
        );
      }

      for (CSVRecord record : parser) {
        totalRows++;

        int rowNumber = (int) record.getRecordNumber() + 1;
        String sku = record.get("product_sku").trim();
        String warehouseCode = record.get("warehouse_code").trim();

        try {
          if (sku.isBlank() || warehouseCode.isBlank()) {
            throw new IllegalArgumentException(
                "Product SKU and warehouse code are required."
            );
          }

          long quantity = Long.parseLong(
              record.get("quantity").trim()
          );

          if (quantity < 0) {
            throw new IllegalArgumentException(
                "Quantity cannot be negative."
            );
          }

          // Resolve these using repositories scoped to the
          // currently authenticated user's account.
          Long ownerId = currentUser.id();
          var product = productRepository
              .findBySkuAndOwner_Id(sku, ownerId)
              .orElseThrow(() ->
                               new IllegalArgumentException("Product SKU not found: " + sku)
              );

          var warehouse = warehouseRepository
              .findByCodeAndOwner_Id(warehouseCode, ownerId)
              .orElseThrow(() ->
                               new IllegalArgumentException(
                                   "Warehouse code not found: " + warehouseCode
                               )
              );
          String bin = record.get("bin").trim();

          inventoryitemService.initializeStock(
              new InitialStockRequest(
                  product.getId(),
                  warehouse.getId(),
                  quantity,
                  bin.isBlank() ? null : bin
              )
          );

          imported++;

        } catch (Exception e) {
          errors.add(
              new InventoryInitialImportResponse.RowError(
                  rowNumber,
                  sku,
                  warehouseCode,
                  e.getMessage() == null
                  ? "Unable to import this row."
                  : e.getMessage()
              )
          );
        }
      }

    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Could not read the uploaded CSV file.", e
      );
    }

    return new InventoryInitialImportResponse(
        totalRows,
        imported,
        totalRows - imported,
        errors
    );
  }

  private Long currentUserId() {
    return currentUser.id();
  }
}