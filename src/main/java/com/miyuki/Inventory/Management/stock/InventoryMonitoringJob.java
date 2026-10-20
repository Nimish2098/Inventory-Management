package com.miyuki.Inventory.Management.stock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryMonitoringJob {

    private static final Logger logger = LoggerFactory.getLogger(InventoryMonitoringJob.class);

    private final LowStockAlertService lowStockAlertService;
    private final MeterRegistry meterRegistry;
    private final Counter monitoringRunsCounter;
    private final Counter alertsCreatedCounter;
    private final Timer monitoringTimer;

    public InventoryMonitoringJob(LowStockAlertService lowStockAlertService, MeterRegistry meterRegistry) {
        this.lowStockAlertService = lowStockAlertService;
        this.meterRegistry = meterRegistry;
        this.monitoringRunsCounter = Counter.builder("inventory.monitoring.runs")
                .description("Number of internal inventory monitoring job executions")
                .register(meterRegistry);
        this.alertsCreatedCounter = Counter.builder("inventory.monitoring.alerts.created")
                .description("Number of low-stock alert events created")
                .register(meterRegistry);
        this.monitoringTimer = Timer.builder("inventory.monitoring.duration")
                .description("Execution duration of internal inventory monitoring job")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${monitor.inventory-interval-ms:300000}")
    public synchronized void monitorInventory() {
        monitoringRunsCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        logger.atInfo()
                .addKeyValue("event", "inventory_monitoring_started")
                .log("Inventory monitoring cycle started");
        try {
            LowStockAlertService.MonitoringResult result = lowStockAlertService.checkInventory();
            alertsCreatedCounter.increment(result.eventsCreated());
            logger.atInfo()
                    .addKeyValue("processed", result.processed())
                    .addKeyValue("failed", result.failed())
                    .addKeyValue("notifications_created", result.eventsCreated())
                    .log("Inventory monitoring cycle completed");
        } finally {
            sample.stop(monitoringTimer);
            logger.atInfo()
                    .addKeyValue("event", "inventory_monitoring_completed")
                    .log("Inventory monitoring cycle completed");
        }
    }
}
