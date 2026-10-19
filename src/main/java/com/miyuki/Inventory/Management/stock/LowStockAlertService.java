package com.miyuki.Inventory.Management.stock;

import com.miyuki.Inventory.Management.stock.DiscordNotificationService.DiscordNotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LowStockAlertService {

    private static final Logger logger = LoggerFactory.getLogger(LowStockAlertService.class);

    private final InventoryitemRepository inventoryRepository;
    private final InventoryAlertStateRepository alertStateRepository;
    private final InventoryMapper inventoryMapper;
    private final DiscordNotificationService notificationService;

    public LowStockAlertService(
            InventoryitemRepository inventoryRepository,
            InventoryAlertStateRepository alertStateRepository,
            InventoryMapper inventoryMapper,
            DiscordNotificationService notificationService) {
        this.inventoryRepository = inventoryRepository;
        this.alertStateRepository = alertStateRepository;
        this.inventoryMapper = inventoryMapper;
        this.notificationService = notificationService;
    }

    public void checkInventory() {
        List<InventoryItem> inventoryItems;
        try {
            inventoryItems = inventoryRepository.findAllForMonitoring();
        } catch (RuntimeException exception) {
            logger.error("Could not load inventory for low-stock monitoring", exception);
            return;
        }

        for (InventoryItem item : inventoryItems) {
            try {
                processItem(item);
            } catch (RuntimeException exception) {
                logger.error("Low-stock monitoring failed for inventory item {}", item.getId(), exception);
            }
        }
    }

    private void processItem(InventoryItem item) {
        boolean lowStock = inventoryMapper.isLowStock(item);
        Optional<InventoryAlertState> existing = alertStateRepository.findById(item.getId());

        if (!lowStock) {
            existing.filter(InventoryAlertState::isLowStock).ifPresent(state -> {
                state.setLowStock(false);
                state.setNotificationSent(false);
                alertStateRepository.save(state);
                logger.info("Low-stock condition recovered for inventory item {}", item.getId());
            });
            return;
        }

        InventoryAlertState state = existing.orElseGet(
                () -> new InventoryAlertState(item.getId(), false, false));
        if (state.isLowStock() && state.isNotificationSent()) {
            return;
        }

        state.setLowStock(true);
        state.setNotificationSent(false);
        state = alertStateRepository.save(state);
        notificationService.sendLowStockAlert(formatMessage(item));
        state.setNotificationSent(true);
        alertStateRepository.save(state);
        logger.info("Low-stock alert delivered for inventory item {}", item.getId());
    }

    private String formatMessage(InventoryItem item) {
        return """
                LOW STOCK ALERT

                Product: %s
                Warehouse: %s
                Current quantity: %d
                Reorder level: %d

                Stock has fallen below the configured reorder level.
                """.formatted(
                item.getProduct().getName(),
                item.getWarehouse().getCode(),
                item.getQuantity(),
                item.getProduct().getReorderLevel());
    }
}
