package com.miyuki.Inventory.Management.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class ExternalProductMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalProductMonitoringService.class);

    private final ProductWatchRepository watchRepository;
    private final List<StoreChecker> storeCheckers;
    private final ProductObservationRecorder observationRecorder;
    private final int batchSize;

    public ExternalProductMonitoringService(
            ProductWatchRepository watchRepository,
            List<StoreChecker> storeCheckers,
            ProductObservationRecorder observationRecorder,
            @Value("${monitor.external.batch-size:50}") int batchSize) {
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("External monitor batch size must be between 1 and 500");
        }
        this.watchRepository = watchRepository;
        this.storeCheckers = storeCheckers;
        this.observationRecorder = observationRecorder;
        this.batchSize = batchSize;
    }

    public MonitorResult monitorEnabledWatches() {
        int pageNumber = 0;
        int checked = 0;
        int failures = 0;
        Page<ProductWatch> page;
        do {
            page = watchRepository.findAllByEnabledTrue(
                    PageRequest.of(pageNumber, batchSize, Sort.by(Sort.Direction.ASC, "id")));
            for (ProductWatch watch : page.getContent()) {
                checked++;
                if (!checkWatch(watch)) {
                    failures++;
                }
            }
            pageNumber++;
        } while (page.hasNext());
        return new MonitorResult(checked, checked - failures, failures);
    }

    private boolean checkWatch(ProductWatch watch) {
        try {
            URI uri = URI.create(watch.getProductUrl());
            StoreChecker checker = storeCheckers.stream()
                    .filter(candidate -> candidate.supports(uri))
                    .findFirst()
                    .orElse(null);
            if (checker == null) {
                logger.atWarn().addKeyValue("watchId", watch.getId())
                        .log("No store checker is registered for this product host");
                return false;
            }

            StoreObservation observation = checker.check(watch);
            if (observation == null) {
                throw new StoreCheckException("Store checker returned no observation");
            }
            observationRecorder.record(watch.getId(), observation);
            logger.atInfo().addKeyValue("watchId", watch.getId())
                    .log("External product observation recorded");
            return true;
        } catch (RuntimeException exception) {
            logger.error("External product check failed for watch {} ({})",
                    watch.getId(), exception.getClass().getSimpleName());
            return false;
        }
    }

    public record MonitorResult(int checked, int successful, int failed) {
    }
}
