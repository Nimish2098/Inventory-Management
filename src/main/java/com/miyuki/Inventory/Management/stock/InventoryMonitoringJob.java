package com.miyuki.Inventory.Management.stock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryMonitoringJob {

    private static final Logger logger = LoggerFactory.getLogger(InventoryMonitoringJob.class);

    private final LowStockAlertService lowStockAlertService;

    public InventoryMonitoringJob(LowStockAlertService lowStockAlertService) {
        this.lowStockAlertService = lowStockAlertService;
    }

    @Scheduled(fixedDelayString = "${monitor.inventory-interval-ms:300000}")
    public synchronized void monitorInventory() {
        logger.atInfo()
                .addKeyValue("event", "inventory_monitoring_started")
                .log("Inventory monitoring cycle started");
        try {
            lowStockAlertService.checkInventory();
        } finally {
            logger.atInfo()
                    .addKeyValue("event", "inventory_monitoring_completed")
                    .log("Inventory monitoring cycle completed");
        }
    }
}
