package com.miyuki.Inventory.Management.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class LowStockAlertService {

    private static final Logger logger = LoggerFactory.getLogger(LowStockAlertService.class);
    private static final int BATCH_SIZE = 100;

    private final InventoryitemRepository inventoryRepository;
    private final InternalLowStockAlertRecorder alertRecorder;

    public LowStockAlertService(
            InventoryitemRepository inventoryRepository,
            InternalLowStockAlertRecorder alertRecorder) {
        this.inventoryRepository = inventoryRepository;
        this.alertRecorder = alertRecorder;
    }

    public MonitoringResult checkInventory() {
        int processed = 0;
        int failed = 0;
        int eventsCreated = 0;
        int pageNumber = 0;
        Page<InventoryItem> page;
        do {
            try {
                page = inventoryRepository.findAllForMonitoring(
                        PageRequest.of(pageNumber, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            } catch (RuntimeException exception) {
                logger.error("Could not load inventory batch for low-stock monitoring", exception);
                return new MonitoringResult(processed, failed + 1, eventsCreated);
            }

            for (InventoryItem item : page.getContent()) {
                processed++;
                try {
                    if (alertRecorder.check(item.getId())) {
                        eventsCreated++;
                    }
                } catch (RuntimeException exception) {
                    failed++;
                    logger.error("Low-stock monitoring failed for inventory item {} ({})",
                            item.getId(), exception.getClass().getSimpleName());
                }
            }
            pageNumber++;
        } while (page.hasNext());
        return new MonitoringResult(processed, failed, eventsCreated);
    }

    public record MonitoringResult(int processed, int failed, int eventsCreated) {
    }
}
