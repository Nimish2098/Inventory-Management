package com.miyuki.Inventory.Management.external;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalProductMonitoringJob {

    private static final Logger logger = LoggerFactory.getLogger(ExternalProductMonitoringJob.class);

    private final ExternalProductMonitoringService monitoringService;
    private final MeterRegistry meterRegistry;
    private final Counter monitoringRunsCounter;
    private final Counter checksAttemptedCounter;
    private final Counter checksSuccessfulCounter;
    private final Counter checksFailedCounter;
    private final Timer monitoringTimer;

    public ExternalProductMonitoringJob(
            ExternalProductMonitoringService monitoringService,
            MeterRegistry meterRegistry) {
        this.monitoringService = monitoringService;
        this.meterRegistry = meterRegistry;
        this.monitoringRunsCounter = Counter.builder("external.monitoring.runs")
                .description("Number of external product monitoring job executions")
                .register(meterRegistry);
        this.checksAttemptedCounter = Counter.builder("external.monitoring.checks.attempted")
                .description("Number of external watch checks attempted")
                .register(meterRegistry);
        this.checksSuccessfulCounter = Counter.builder("external.monitoring.checks.successful")
                .description("Number of external watch checks succeeded")
                .register(meterRegistry);
        this.checksFailedCounter = Counter.builder("external.monitoring.checks.failed")
                .description("Number of external watch checks failed")
                .register(meterRegistry);
        this.monitoringTimer = Timer.builder("external.monitoring.duration")
                .description("Execution duration of external product monitoring job")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${monitor.external.interval-ms:900000}")
    public synchronized void monitorExternalProducts() {
        monitoringRunsCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        logger.atInfo().addKeyValue("event", "external_product_monitoring_started")
                .log("External product monitoring cycle started");
        try {
            ExternalProductMonitoringService.MonitorResult result =
                    monitoringService.monitorEnabledWatches();
            checksAttemptedCounter.increment(result.checked());
            checksSuccessfulCounter.increment(result.successful());
            checksFailedCounter.increment(result.failed());
            logger.atInfo()
                    .addKeyValue("processed", result.checked())
                    .addKeyValue("successful", result.successful())
                    .addKeyValue("failed", result.failed())
                    .log("External product monitoring cycle completed");
        } finally {
            sample.stop(monitoringTimer);
            logger.atInfo().addKeyValue("event", "external_product_monitoring_finished")
                    .log("External product monitoring cycle completed");
        }
    }
}
