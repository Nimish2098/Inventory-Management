package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.dto.StockAdjustmentRequest;
import com.miyuki.Inventory.Management.stock.dto.StockTransferRequest;
import com.miyuki.Inventory.Management.stock.dto.mapper.InventoryMapper;
import com.miyuki.Inventory.Management.stock.model.StockMovementType;
import com.miyuki.Inventory.Management.stock.repository.InventoryitemRepository;
import com.miyuki.Inventory.Management.stock.repository.StockMovementRepository;
import com.miyuki.Inventory.Management.stock.service.InventoryitemService;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryitemServiceTest
{

  @Mock
  private InventoryitemRepository inventoryitemRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private WarehouseRepository warehouseRepository;

  @Mock
  private StockMovementRepository stockMovementRepository;

  @InjectMocks
  private InventoryitemService inventoryitemService;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private InventoryMapper inventoryMapper;

  @Test
  void adjustStock_shouldRejectInvalidQuantity()
  {
    // Adapt this request constructor to your actual DTO.
    var request = new StockAdjustmentRequest(
        1L, 1L, 0L, StockMovementType.INBOUND
    );

    assertThrows(
        RuntimeException.class,
        () -> inventoryitemService.adjustStock(request)
    );
  }

  @Test
  void transferStock_shouldRejectTransferToSameWarehouse()
  {
    var request = new StockTransferRequest(
        1L, 1L, 1L, 10L
    );

    assertThrows(
        RuntimeException.class,
        () -> inventoryitemService.transferStock(request)
    );
  }

  @Test
  void transferStock_shouldRejectNonPositiveQuantity()
  {
    var request = new StockTransferRequest(
        1L, 1L, 2L, 0L
    );

    assertThrows(
        RuntimeException.class,
        () -> inventoryitemService.transferStock(request)
    );
  }



  @Test
  void getInventory_shouldCallRepositoryWithWarehouseFilter() {
    when(currentUser.id()).thenReturn(1L);

    when(inventoryitemRepository
             .findByWarehouse_IdAndProduct_Owner_IdAndWarehouse_Owner_Id(
                 1L, 1L, 1L
             ))
        .thenReturn(java.util.List.of());
    assertNotNull(currentUser, "CurrentUser mock is null");
    assertEquals(1L, currentUser.id(), "CurrentUser ID was not stubbed");
    assertNotNull(inventoryitemService, "Service was not initialized");
    assertNotNull(inventoryitemService.getInventory(1L));

    verify(inventoryitemRepository)
        .findByWarehouse_IdAndProduct_Owner_IdAndWarehouse_Owner_Id(
            1L, 1L, 1L
        );
  }

}