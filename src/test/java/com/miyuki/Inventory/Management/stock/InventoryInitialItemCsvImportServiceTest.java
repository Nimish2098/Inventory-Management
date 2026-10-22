package com.miyuki.Inventory.Management.stock;


import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.dto.InventoryInitialImportResponse;
import com.miyuki.Inventory.Management.stock.service.InventoryInitialItemCsvImportService;
import com.miyuki.Inventory.Management.stock.service.InventoryitemService;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryInitialItemCsvImportServiceTest {

  @Mock
  private InventoryitemService inventoryitemService;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private WarehouseRepository warehouseRepository;

  @InjectMocks
  private InventoryInitialItemCsvImportService csvImportService;

  @Test
  void importCsv_shouldRejectEmptyFile() {
    var file = new MockMultipartFile(
        "file",
        "inventory.csv",
        "text/csv",
        new byte[0]
    );

    assertThrows(
        IllegalArgumentException.class,
        () -> csvImportService.importCsv(file)
    );
  }

  @Test
  void importCsv_shouldRejectNonCsvFile() {
    var file = new MockMultipartFile(
        "file",
        "inventory.txt",
        "text/plain",
        "invalid".getBytes()
    );

    assertThrows(
        IllegalArgumentException.class,
        () -> csvImportService.importCsv(file)
    );
  }

  @Test
  void importCsv_shouldRejectMissingRequiredHeaders() {
    String csv = """
                product_sku,quantity
                LAP-001,50
                """;

    var file = new MockMultipartFile(
        "file",
        "inventory.csv",
        "text/csv",
        csv.getBytes()
    );

    assertThrows(
        IllegalArgumentException.class,
        () -> csvImportService.importCsv(file)
    );
  }

  @Test
  void importCsv_shouldRejectNegativeQuantity() {
    String csv = """
                product_sku,warehouse_code,quantity,bin
                LAP-001,WH-00001,-5,A-01
                """;

    var file = new MockMultipartFile(
        "file",
        "inventory.csv",
        "text/csv",
        csv.getBytes()
    );

    /*
     * The importer should report a row-level validation error
     * instead of initializing stock for this row.
     *
     * Connect this assertion to the actual response contract
     * and repository lookup behavior in your implementation.
     */
    assertDoesNotThrow(() -> {
      try {
        InventoryInitialImportResponse response =
            csvImportService.importCsv(file);

        assertEquals(1, response.totalRows());
        assertEquals(0, response.imported());
        assertEquals(1, response.failed());
        assertEquals(1, response.errors().size());
      } catch (IllegalArgumentException e) {
        // Accept only if your implementation rejects the whole file.
        throw e;
      }
    });

    verifyNoInteractions(inventoryitemService);
  }
}