package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.stock.DiscordNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductNotificationOutboxTest {

    private final ProductNotificationEventRepository eventRepository =
            mock(ProductNotificationEventRepository.class);
    private final DiscordNotificationService discordService = mock(DiscordNotificationService.class);
    private final ProductNotificationEventStatusService statusService =
            mock(ProductNotificationEventStatusService.class);
    private final ProductNotificationOutbox outbox =
            new ProductNotificationOutbox(eventRepository, discordService, statusService);

    @Test
    void successfulDeliveryMarksPersistedEventDelivered() {
        ProductNotificationEvent event = event();
        when(eventRepository.findDueEventIds(any(), anyInt(), any(Pageable.class))).thenReturn(List.of(7L));
        when(statusService.claim(anyLong(), any())).thenReturn(true);
        when(eventRepository.findById(7L)).thenReturn(java.util.Optional.of(event));

        outbox.dispatchDue();

        verify(discordService).sendNotification(any());
        verify(statusService).markDelivered(anyLong(), any());
    }

    @Test
    void deliveryFailureLeavesEventPendingForRetry() {
        ProductNotificationEvent event = event();
        when(eventRepository.findDueEventIds(any(), anyInt(), any(Pageable.class))).thenReturn(List.of(7L));
        when(statusService.claim(anyLong(), any())).thenReturn(true);
        when(eventRepository.findById(7L)).thenReturn(java.util.Optional.of(event));
        org.mockito.Mockito.doThrow(new DiscordNotificationService.DiscordNotificationException(
                        "webhook unavailable", true, java.util.Optional.empty()))
                .when(discordService).sendNotification(any());

        outbox.dispatchDue();

        verify(statusService, never()).markDelivered(anyLong(), any());
        verify(statusService).markFailed(
                anyLong(), any(), any(), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.nullable(java.time.Duration.class));
    }

    @Test
    void permanentConfigurationFailureIsNotRetried() {
        ProductNotificationEvent event = event();
        when(eventRepository.findDueEventIds(any(), anyInt(), any(Pageable.class))).thenReturn(List.of(7L));
        when(statusService.claim(anyLong(), any())).thenReturn(true);
        when(eventRepository.findById(7L)).thenReturn(java.util.Optional.of(event));
        org.mockito.Mockito.doThrow(new IllegalStateException("Set DISCORD_WEBHOOK_URL"))
                .when(discordService).sendNotification(any());

        outbox.dispatchDue();

        verify(statusService).markFailed(
                anyLong(), any(), any(), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.nullable(java.time.Duration.class));
    }

    private ProductNotificationEvent event() {
        ProductWatch watch = new ProductWatch();
        watch.setId(5L);
        watch.setProductName("Widget");
        watch.setProductUrl("https://shop.example/widget");
        ProductNotificationEvent event = new ProductNotificationEvent(
                watch, ProductEventType.PRICE_DROP,
                new BigDecimal("20.00"), new BigDecimal("18.00"), new BigDecimal("19.00"),
                Instant.now(), "watch:5:PRICE_DROP:2");
        event.setId(7L);
        return event;
    }
}
