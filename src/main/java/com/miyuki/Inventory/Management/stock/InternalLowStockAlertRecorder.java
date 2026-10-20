package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.external.ProductNotificationEvent;
import com.miyuki.Inventory.Management.external.ProductNotificationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalLowStockAlertRecorder {

    private final InventoryitemRepository inventoryRepository;
    private final InventoryAlertStateRepository alertStateRepository;
    private final ProductNotificationEventRepository notificationRepository;
    private final InventoryMapper inventoryMapper;

    public InternalLowStockAlertRecorder(
            InventoryitemRepository inventoryRepository,
            InventoryAlertStateRepository alertStateRepository,
            ProductNotificationEventRepository notificationRepository,
            InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.alertStateRepository = alertStateRepository;
        this.notificationRepository = notificationRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Transactional
    public boolean check(Long inventoryItemId) {
        InventoryItem item = inventoryRepository.findForMonitoringById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory item " + inventoryItemId + " not found"));
        InventoryAlertState state = alertStateRepository
                .findByInventoryItemIdForUpdate(inventoryItemId)
                .orElse(null);
        boolean lowStock = inventoryMapper.isLowStock(item);

        if (!lowStock) {
            if (state != null && state.isLowStock()) {
                state.setLowStock(false);
                state.setNotificationQueued(false);
                alertStateRepository.save(state);
            }
            return false;
        }
        if (state != null && state.isLowStock()) {
            return false;
        }

        long episode = state == null || state.getEpisodeNumber() == null
                ? 1L
                : state.getEpisodeNumber() + 1;
        if (state == null) {
            state = new InventoryAlertState(inventoryItemId, true, episode);
        } else {
            state.setLowStock(true);
            state.setEpisodeNumber(episode);
        }
        state.setNotificationQueued(true);
        alertStateRepository.save(state);
        notificationRepository.save(new ProductNotificationEvent(item, episode, java.time.Instant.now()));
        return true;
    }
}
