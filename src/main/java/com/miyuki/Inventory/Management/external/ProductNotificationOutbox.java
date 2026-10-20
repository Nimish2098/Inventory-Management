package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.stock.DiscordNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ProductNotificationOutbox {

    private static final Logger logger = LoggerFactory.getLogger(ProductNotificationOutbox.class);
    private static final int DISPATCH_BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 8;

    private final ProductNotificationEventRepository eventRepository;
    private final DiscordNotificationService discordNotificationService;
    private final ProductNotificationEventStatusService statusService;

    public ProductNotificationOutbox(
            ProductNotificationEventRepository eventRepository,
            DiscordNotificationService discordNotificationService,
            ProductNotificationEventStatusService statusService) {
        this.eventRepository = eventRepository;
        this.discordNotificationService = discordNotificationService;
        this.statusService = statusService;
    }

    public DispatchResult dispatchDue() {
        Instant start = Instant.now();
        eventRepository.exhaustExpiredClaims(start, MAX_ATTEMPTS);
        List<Long> eventIds = eventRepository.findDueEventIds(
                start, MAX_ATTEMPTS, PageRequest.of(0, DISPATCH_BATCH_SIZE));
        int attempted = 0;
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (Long eventId : eventIds) {
            Instant now = Instant.now();
            if (!statusService.claim(eventId, now)) {
                skipped++;
                continue;
            }
            attempted++;
            ProductNotificationEvent event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                logger.error("Claimed notification event {} disappeared before delivery", eventId);
                failed++;
                continue;
            }

            try {
                discordNotificationService.sendNotification(formatMessage(event));
                statusService.markDelivered(eventId, Instant.now());
                sent++;
                logger.atInfo().addKeyValue("eventId", eventId)
                        .log("Notification event delivered");
            } catch (RuntimeException exception) {
                failed++;
                boolean retryable = exception instanceof DiscordNotificationService.DiscordNotificationException deliveryException
                        ? deliveryException.isRetryable()
                        : false;
                var retryAfter = exception instanceof DiscordNotificationService.DiscordNotificationException deliveryException
                        ? deliveryException.getRetryAfter().orElse(null)
                        : null;
                statusService.markFailed(
                        eventId,
                        Instant.now(),
                        safeError(exception),
                        retryable,
                        retryAfter);
                logger.error("Notification event {} delivery failed ({})",
                        eventId, exception.getClass().getSimpleName());
            }
        }
        return new DispatchResult(eventIds.size(), attempted, sent, failed, skipped);
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replaceAll("https?://\\S+", "[redacted-url]");
    }

    private String formatMessage(ProductNotificationEvent event) {
        return switch (event.getEventType()) {
            case PRICE_DROP -> """
                    PRICE DROP

                    Product: %s
                    Previous price: %s
                    Current price: %s
                    Target price: %s

                    The observed product price has decreased.
                    """.formatted(
                    event.getProductName(),
                    event.getPreviousPrice().toPlainString(),
                    event.getCurrentPrice().toPlainString(),
                    priceOrNotSet(event));
            case TARGET_PRICE_REACHED -> """
                    TARGET PRICE REACHED

                    Product: %s
                    Previous price: %s
                    Current price: %s
                    Target price: %s

                    The product price crossed to or below your target.
                    """.formatted(
                    event.getProductName(),
                    event.getPreviousPrice().toPlainString(),
                    event.getCurrentPrice().toPlainString(),
                    priceOrNotSet(event));
            case RESTOCK -> """
                    BACK IN STOCK

                    Product: %s
                    Current price: %s

                    The product is available again.
                    """.formatted(
                    event.getProductName(),
                    event.getCurrentPrice().toPlainString());
            case INTERNAL_LOW_STOCK -> """
                    LOW STOCK ALERT

                    Product: %s
                    Warehouse: %s
                    Current quantity: %d
                    Reorder level: %d

                    Stock has fallen below the configured reorder level.
                    """.formatted(
                    event.getProductName(),
                    event.getWarehouseCode(),
                    event.getCurrentQuantity(),
                    event.getReorderLevel());
        };
    }

    private String priceOrNotSet(ProductNotificationEvent event) {
        BigDecimal targetPrice = event.getTargetPrice();
        return targetPrice == null ? "not set" : targetPrice.toPlainString();
    }

    public record DispatchResult(int selected, int attempted, int sent, int failed, int skipped) {
    }
}
