package com.miyuki.Inventory.Management.external;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductNotificationEventStatusServiceTest {

    private final ProductNotificationEventRepository eventRepository =
            mock(ProductNotificationEventRepository.class);
    private final ProductNotificationEventStatusService service =
            new ProductNotificationEventStatusService(eventRepository);

    @Test
    void permanentFailureExhaustsTheEventImmediately() {
        ProductNotificationEvent event = event();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        service.markFailed(1L, Instant.now(), "HTTP 401", false, null);

        assertEquals(NotificationDeliveryStatus.EXHAUSTED, event.getStatus());
        assertEquals("HTTP 401", event.getLastErrorSummary());
        verify(eventRepository).findById(1L);
    }

    @Test
    void transientFailureUsesBackoffAndKeepsEventRetryable() {
        ProductNotificationEvent event = event();
        event.setAttemptCount(1);
        Instant now = Instant.now();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        service.markFailed(1L, now, "temporary failure", true, null);

        assertEquals(NotificationDeliveryStatus.FAILED, event.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(event.getNextAttemptAt().isAfter(now));
    }

    @Test
    void rateLimitDelayIsAppliedAsReturned() {
        ProductNotificationEvent event = event();
        Instant now = Instant.now();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        service.markFailed(1L, now, "rate limited", true, java.time.Duration.ofSeconds(37));

        assertEquals(now.plusSeconds(37), event.getNextAttemptAt());
    }

    private ProductNotificationEvent event() {
        ProductWatch watch = new ProductWatch();
        watch.setId(2L);
        watch.setProductName("Example");
        ProductNotificationEvent event = new ProductNotificationEvent(
                watch,
                ProductEventType.PRICE_DROP,
                new BigDecimal("10"),
                new BigDecimal("9"),
                null,
                Instant.now(),
                "test:event");
        event.setId(1L);
        event.setStatus(NotificationDeliveryStatus.SENDING);
        return event;
    }
}
