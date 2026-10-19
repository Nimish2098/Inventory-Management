package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LowStockAlertServiceTest {

    private final InventoryitemRepository inventoryRepository = mock(InventoryitemRepository.class);
    private final InventoryAlertStateRepository alertStateRepository = mock(InventoryAlertStateRepository.class);
    private final DiscordNotificationService notificationService = mock(DiscordNotificationService.class);
    private final InventoryMapper inventoryMapper = new InventoryMapper();

    private final LowStockAlertService service = new LowStockAlertService(
            inventoryRepository, alertStateRepository, inventoryMapper, notificationService);

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setName("Tea");
        product.setReorderLevel(20L);
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-EAST");
        item = new InventoryItem();
        item.setId(8L);
        item.setProduct(product);
        item.setWarehouse(warehouse);
        item.setQuantity(15L);
        when(alertStateRepository.save(any(InventoryAlertState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void inventoryAboveReorderLevelDoesNotNotify() {
        item.setQuantity(21L);
        when(inventoryRepository.findAllForMonitoring()).thenReturn(List.of(item));

        service.checkInventory();

        verify(notificationService, never()).sendLowStockAlert(any());
        verify(alertStateRepository, never()).save(any());
    }

    @Test
    void lowStockNotifiesOnceWhileConditionIsUnchanged() {
        when(inventoryRepository.findAllForMonitoring()).thenReturn(List.of(item));
        when(alertStateRepository.findById(8L)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(new InventoryAlertState(8L, true, true)));
        long quantityBefore = item.getQuantity();

        service.checkInventory();
        service.checkInventory();

        verify(notificationService, times(1)).sendLowStockAlert(any());
        assertEquals(quantityBefore, item.getQuantity());
    }

    @Test
    void notificationContainsTheActualProductWarehouseAndQuantities() {
        when(inventoryRepository.findAllForMonitoring()).thenReturn(List.of(item));
        when(alertStateRepository.findById(8L)).thenReturn(Optional.empty());
        org.mockito.ArgumentCaptor<String> message = org.mockito.ArgumentCaptor.forClass(String.class);

        service.checkInventory();

        verify(notificationService).sendLowStockAlert(message.capture());
        assertTrue(message.getValue().contains("Product: Tea"));
        assertTrue(message.getValue().contains("Warehouse: WH-EAST"));
        assertTrue(message.getValue().contains("Current quantity: 15"));
        assertTrue(message.getValue().contains("Reorder level: 20"));
    }

    @Test
    void failedDeliveryRemainsPendingAndIsRetried() {
        InventoryAlertState pending = new InventoryAlertState(8L, true, false);
        when(inventoryRepository.findAllForMonitoring()).thenReturn(List.of(item));
        when(alertStateRepository.findById(8L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pending));
        org.mockito.Mockito.doThrow(new DiscordNotificationService.DiscordNotificationException(
                        "Discord notification failed"))
                .doNothing()
                .when(notificationService).sendLowStockAlert(any());

        service.checkInventory();
        service.checkInventory();

        verify(notificationService, times(2)).sendLowStockAlert(any());
        assertTrue(pending.isNotificationSent());
    }

    @Test
    void recoveryThenLowStockCreatesANewAlert() {
        when(inventoryRepository.findAllForMonitoring()).thenReturn(List.of(item));
        when(alertStateRepository.findById(8L))
                .thenReturn(Optional.of(new InventoryAlertState(8L, true, true)))
                .thenReturn(Optional.of(new InventoryAlertState(8L, false, false)));

        item.setQuantity(25L);
        service.checkInventory();
        item.setQuantity(15L);
        service.checkInventory();

        verify(notificationService, times(1)).sendLowStockAlert(any());
        verify(alertStateRepository, times(3)).save(any(InventoryAlertState.class));
    }

    @Test
    void failedInventoryRecordDoesNotPreventOtherRecordsFromBeingChecked() {
        InventoryItem nextItem = new InventoryItem();
        nextItem.setId(9L);
        nextItem.setProduct(item.getProduct());
        nextItem.setWarehouse(item.getWarehouse());
        nextItem.setQuantity(10L);
        when(inventoryRepository.findAllForMonitoring()).thenReturn(List.of(item, nextItem));
        when(alertStateRepository.findById(8L)).thenThrow(new IllegalStateException("database read failed"));
        when(alertStateRepository.findById(9L)).thenReturn(Optional.empty());

        service.checkInventory();

        verify(notificationService).sendLowStockAlert(any());
    }
}
