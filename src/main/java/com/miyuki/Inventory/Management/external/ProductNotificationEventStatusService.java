package com.miyuki.Inventory.Management.external;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductNotificationEventStatusService {

    private static final int MAX_ATTEMPTS = 8;
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(2);
    private final ProductNotificationEventRepository eventRepository;

    public ProductNotificationEventStatusService(ProductNotificationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public boolean claim(Long eventId, Instant now) {
        return eventRepository.claimIfDue(
                eventId, now, now.plus(CLAIM_LEASE), MAX_ATTEMPTS) == 1;
    }

    @Transactional
    public void markDelivered(Long eventId, Instant deliveredAt) {
        ProductNotificationEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Notification event no longer exists"));
        event.setDeliveredAt(deliveredAt);
        event.setStatus(NotificationDeliveryStatus.SENT);
        event.setLastErrorSummary(null);
        event.setLastAttemptAt(deliveredAt);
    }

    @Transactional
    public void markFailed(
            Long eventId,
            Instant now,
            String safeError,
            boolean retryable,
            Duration retryAfter) {
        ProductNotificationEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Notification event no longer exists"));
        event.setLastAttemptAt(now);
        event.setLastErrorSummary(safeError.substring(0, Math.min(safeError.length(), 500)));

        if (!retryable || event.getAttemptCount() >= MAX_ATTEMPTS) {
            event.setStatus(NotificationDeliveryStatus.EXHAUSTED);
            return;
        }

        event.setStatus(NotificationDeliveryStatus.FAILED);
        event.setNextAttemptAt(now.plus(retryAfter == null ? retryDelay(event.getAttemptCount()) : retryAfter));
    }

    private Duration retryDelay(int attempt) {
        long backoffSeconds = Math.min(21_600L, 15L << Math.min(Math.max(attempt - 1, 0), 10));
        double jitterFactor = ThreadLocalRandom.current().nextDouble(0.8, 1.2);
        return Duration.ofMillis((long) (backoffSeconds * 1000 * jitterFactor));
    }
}
