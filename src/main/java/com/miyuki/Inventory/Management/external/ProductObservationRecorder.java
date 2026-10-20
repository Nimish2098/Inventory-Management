package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductObservationRecorder {

    private final ProductWatchRepository watchRepository;
    private final ProductObservationRepository observationRepository;
    private final ProductNotificationEventRepository notificationRepository;

    public ProductObservationRecorder(
            ProductWatchRepository watchRepository,
            ProductObservationRepository observationRepository,
            ProductNotificationEventRepository notificationRepository) {
        this.watchRepository = watchRepository;
        this.observationRepository = observationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void record(Long watchId, StoreObservation current) {
        ProductWatch watch = watchRepository.findByIdForUpdate(watchId)
                .orElseThrow(() -> new ResourceNotFoundException("Product watch " + watchId + " not found"));
        if (!watch.isEnabled()) {
            return;
        }

        ProductObservation previous = observationRepository
                .findTopByWatch_IdOrderByObservedAtDescIdDesc(watchId).orElse(null);
        List<ProductNotificationEvent> events = new ArrayList<>(3);

        if (previous != null) {
            if (current.price().compareTo(previous.getObservedPrice()) < 0) {
                events.add(new ProductNotificationEvent(
                        watch, ProductEventType.PRICE_DROP, previous.getObservedPrice(),
                        current.price(), watch.getTargetPrice(), current.observedAt(),
                        eventKey(watch, ProductEventType.PRICE_DROP, previous)));
            }
            if (isTargetPriceCrossed(previous, current, watch)) {
                events.add(new ProductNotificationEvent(
                        watch, ProductEventType.TARGET_PRICE_REACHED, previous.getObservedPrice(),
                        current.price(), watch.getTargetPrice(), current.observedAt(),
                        eventKey(watch, ProductEventType.TARGET_PRICE_REACHED, previous)));
            }
            if (previous.getStockStatus() == StockStatus.OUT_OF_STOCK
                    && current.stockStatus() == StockStatus.IN_STOCK) {
                events.add(new ProductNotificationEvent(
                        watch, ProductEventType.RESTOCK, previous.getObservedPrice(),
                        current.price(), watch.getTargetPrice(), current.observedAt(),
                        eventKey(watch, ProductEventType.RESTOCK, previous)));
            }
        }

        observationRepository.save(new ProductObservation(
                watch, current.price(), current.stockStatus(), watch.getTargetPrice(), current.observedAt()));
        watch.setLatestObservedPrice(current.price());
        watch.setLatestStockStatus(current.stockStatus());
        watch.setLastSuccessfulCheck(current.observedAt());
        watchRepository.save(watch);
        if (!events.isEmpty()) {
            notificationRepository.saveAll(events);
        }
    }

    private String eventKey(
            ProductWatch watch,
            ProductEventType eventType,
            ProductObservation previous) {
        String previousId = previous.getId() == null
                ? previous.getObservedAt().toString()
                : previous.getId().toString();
        return "watch:" + watch.getId() + ":" + eventType + ":" + previousId;
    }

    private boolean isTargetPriceCrossed(
            ProductObservation previous,
            StoreObservation current,
            ProductWatch watch) {
        return previous.getTargetPriceAtObservation() != null
                && watch.getTargetPrice() != null
                && previous.getTargetPriceAtObservation().compareTo(watch.getTargetPrice()) == 0
                && previous.getObservedPrice().compareTo(watch.getTargetPrice()) > 0
                && current.price().compareTo(watch.getTargetPrice()) <= 0;
    }
}
