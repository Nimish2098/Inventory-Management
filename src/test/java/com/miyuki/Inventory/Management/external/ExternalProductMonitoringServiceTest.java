package com.miyuki.Inventory.Management.external;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalProductMonitoringServiceTest {

    private final ProductWatchRepository watchRepository = mock(ProductWatchRepository.class);
    private final ProductObservationRecorder recorder = mock(ProductObservationRecorder.class);

    @Test
    void failedWatchCheckDoesNotStopLaterWatchesAndDoesNotRecordFailure() {
        ProductWatch failingWatch = watch(1L, "https://shop.example/one");
        ProductWatch validWatch = watch(2L, "https://shop.example/two");
        StoreChecker checker = new StoreChecker() {
            @Override
            public boolean supports(java.net.URI uri) {
                return true;
            }

            @Override
            public StoreObservation check(ProductWatch watch) {
                if (watch.getId().equals(1L)) {
                    throw new StoreCheckException("fixture failure");
                }
                return new StoreObservation(
                        new BigDecimal("19.99"), StockStatus.IN_STOCK, Instant.now());
            }
        };
        when(watchRepository.findAllByEnabledTrue(any())).thenReturn(
                new PageImpl<>(List.of(failingWatch, validWatch),
                        PageRequest.of(0, 10, Sort.by("id")), 2));

        ExternalProductMonitoringService service = new ExternalProductMonitoringService(
                watchRepository, List.of(checker), recorder, 10);
        service.monitorEnabledWatches();

        verify(recorder, never()).record(eq(1L), any());
        verify(recorder).record(eq(2L), any());
    }

    @Test
    void noRegisteredCheckerDoesNotFetchOrRecordWatch() {
        ProductWatch watch = watch(3L, "https://shop.example/three");
        when(watchRepository.findAllByEnabledTrue(any())).thenReturn(
                new PageImpl<>(List.of(watch)));

        ExternalProductMonitoringService service = new ExternalProductMonitoringService(
                watchRepository, List.of(), recorder, 10);
        service.monitorEnabledWatches();

        verify(recorder, never()).record(any(), any());
    }

    private ProductWatch watch(Long id, String url) {
        ProductWatch watch = new ProductWatch();
        watch.setId(id);
        watch.setProductName("Fixture product");
        watch.setProductUrl(url);
        watch.setEnabled(true);
        return watch;
    }
}
