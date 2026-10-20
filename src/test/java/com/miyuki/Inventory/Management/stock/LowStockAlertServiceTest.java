package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.external.ProductNotificationEvent;
import com.miyuki.Inventory.Management.external.ProductNotificationEventRepository;
import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LowStockAlertServiceTest {

    private final InventoryitemRepository inventoryRepository = mock(InventoryitemRepository.class);
    private final InventoryAlertStateRepository alertStateRepository = mock(InventoryAlertStateRepository.class);
    private final ProductNotificationEventRepository notificationRepository =
            mock(ProductNotificationEventRepository.class);
    private final InternalLowStockAlertRecorder recorder = new InternalLowStockAlertRecorder(
            inventoryRepository, alertStateRepository, notificationRepository, new InventoryMapper());

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
        when(inventoryRepository.findForMonitoringById(8L)).thenReturn(Optional.of(item));
        when(alertStateRepository.save(any(InventoryAlertState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void inventoryAboveReorderLevelDoesNotCreateAlert() {
        item.setQuantity(21L);
        when(alertStateRepository.findByInventoryItemIdForUpdate(8L)).thenReturn(Optional.empty());

        assertFalse(recorder.check(8L));

        verify(notificationRepository, never()).save(any(ProductNotificationEvent.class));
        verify(alertStateRepository, never()).save(any(InventoryAlertState.class));
    }

    @Test
    void lowStockTransitionPersistsOneEventWithoutChangingQuantity() {
        when(alertStateRepository.findByInventoryItemIdForUpdate(8L)).thenReturn(Optional.empty());
        long quantity = item.getQuantity();

        assertTrue(recorder.check(8L));

        org.mockito.ArgumentCaptor<ProductNotificationEvent> event =
                org.mockito.ArgumentCaptor.forClass(ProductNotificationEvent.class);
        verify(notificationRepository).save(event.capture());
        assertEquals("inventory:8:low-stock:1", event.getValue().getEventKey());
        assertEquals(quantity, item.getQuantity());
    }

    @Test
    void unchangedLowStockEpisodeDoesNotCreateDuplicateEvents() {
        when(alertStateRepository.findByInventoryItemIdForUpdate(8L))
                .thenReturn(Optional.of(new InventoryAlertState(8L, true, 1L)));

        assertFalse(recorder.check(8L));
        assertFalse(recorder.check(8L));

        verify(notificationRepository, never()).save(any(ProductNotificationEvent.class));
    }

    @Test
    void recoveryThenAnotherLowTransitionIncrementsEpisodeKey() {
        InventoryAlertState state = new InventoryAlertState(8L, true, 1L);
        when(alertStateRepository.findByInventoryItemIdForUpdate(8L))
                .thenReturn(Optional.of(state))
                .thenReturn(Optional.of(state));
        item.setQuantity(20L);

        assertFalse(recorder.check(8L));

        item.setQuantity(15L);
        assertTrue(recorder.check(8L));

        org.mockito.ArgumentCaptor<ProductNotificationEvent> event =
                org.mockito.ArgumentCaptor.forClass(ProductNotificationEvent.class);
        verify(notificationRepository).save(event.capture());
        assertEquals("inventory:8:low-stock:2", event.getValue().getEventKey());
    }

    @Test
    void failedItemDoesNotPreventOtherInventoryFromBeingProcessed() {
        InventoryItem other = new InventoryItem();
        other.setId(9L);
        when(inventoryRepository.findAllForMonitoring(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(item, other)));
        InternalLowStockAlertRecorder failingRecorder = mock(InternalLowStockAlertRecorder.class);
        when(failingRecorder.check(8L)).thenThrow(new IllegalStateException("recording error"));

        LowStockAlertService service = new LowStockAlertService(inventoryRepository, failingRecorder);
        LowStockAlertService.MonitoringResult result = service.checkInventory();

        assertEquals(2, result.processed());
        assertEquals(1, result.failed());
        verify(failingRecorder).check(8L);
        verify(failingRecorder).check(9L);
    }
}
