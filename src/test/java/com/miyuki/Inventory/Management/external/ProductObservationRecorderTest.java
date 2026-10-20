package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductObservationRecorderTest {

    private final ProductWatchRepository watchRepository = mock(ProductWatchRepository.class);
    private final ProductObservationRepository observationRepository = mock(ProductObservationRepository.class);
    private final ProductNotificationEventRepository eventRepository =
            mock(ProductNotificationEventRepository.class);
    private final ProductObservationRecorder recorder = new ProductObservationRecorder(
            watchRepository, observationRepository, eventRepository);

    private ProductWatch watch;

    @BeforeEach
    void setUp() {
        watch = new ProductWatch();
        watch.setId(12L);
        watch.setOwner(new UserAccount());
        watch.setProductName("Headphones");
        watch.setProductUrl("https://shop.example/headphones");
        watch.setTargetPrice(new BigDecimal("4000"));
        watch.setEnabled(true);
        when(watchRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(watch));
    }

    @Test
    void firstObservationCreatesBaselineWithoutNotifications() {
        when(observationRepository.findTopByWatch_IdOrderByObservedAtDescIdDesc(12L))
                .thenReturn(Optional.empty());

        recorder.record(12L, observation("4999", StockStatus.OUT_OF_STOCK));

        verify(observationRepository).save(any(ProductObservation.class));
        verify(eventRepository, never()).saveAll(any());
        assertEquals(new BigDecimal("4999"), watch.getLatestObservedPrice());
        assertEquals(StockStatus.OUT_OF_STOCK, watch.getLatestStockStatus());
    }

    @Test
    void priceDropAndTargetCrossingCreateCorrespondingEvents() {
        when(observationRepository.findTopByWatch_IdOrderByObservedAtDescIdDesc(12L))
                .thenReturn(Optional.of(previous("4999", StockStatus.IN_STOCK, "4000")));

        recorder.record(12L, observation("3999", StockStatus.IN_STOCK));

        ArgumentCaptor<List<ProductNotificationEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertEquals(List.of(ProductEventType.PRICE_DROP, ProductEventType.TARGET_PRICE_REACHED),
                captor.getValue().stream().map(ProductNotificationEvent::getEventType).toList());
    }

    @Test
    void configuringTargetDoesNotAlertImmediatelyForAnAlreadyBelowTargetPrice() {
        when(observationRepository.findTopByWatch_IdOrderByObservedAtDescIdDesc(12L))
                .thenReturn(Optional.of(previous("3500", StockStatus.IN_STOCK, null)));

        recorder.record(12L, observation("3400", StockStatus.IN_STOCK));

        ArgumentCaptor<List<ProductNotificationEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertEquals(List.of(ProductEventType.PRICE_DROP),
                captor.getValue().stream().map(ProductNotificationEvent::getEventType).toList());
    }

    @Test
    void restockTransitionCreatesOneEventAndUnchangedStatusDoesNot() {
        when(observationRepository.findTopByWatch_IdOrderByObservedAtDescIdDesc(12L))
                .thenReturn(Optional.of(previous("4500", StockStatus.OUT_OF_STOCK, "4000")));

        recorder.record(12L, observation("4500", StockStatus.IN_STOCK));

        ArgumentCaptor<List<ProductNotificationEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertEquals(List.of(ProductEventType.RESTOCK),
                captor.getValue().stream().map(ProductNotificationEvent::getEventType).toList());
    }

    @Test
    void targetAlreadyConfiguredNeedsARealAboveToBelowCrossing() {
        when(observationRepository.findTopByWatch_IdOrderByObservedAtDescIdDesc(12L))
                .thenReturn(Optional.of(previous("3900", StockStatus.IN_STOCK, "4000")));

        recorder.record(12L, observation("3800", StockStatus.IN_STOCK));

        ArgumentCaptor<List<ProductNotificationEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertEquals(List.of(ProductEventType.PRICE_DROP),
                captor.getValue().stream().map(ProductNotificationEvent::getEventType).toList());
    }

    private ProductObservation previous(String price, StockStatus status, String target) {
        return new ProductObservation(
                watch,
                new BigDecimal(price),
                status,
                target == null ? null : new BigDecimal(target),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private StoreObservation observation(String price, StockStatus status) {
        return new StoreObservation(
                new BigDecimal(price), status, Instant.parse("2026-01-01T00:15:00Z"));
    }
}
