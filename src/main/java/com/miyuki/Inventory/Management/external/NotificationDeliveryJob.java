package com.miyuki.Inventory.Management.external;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryJob {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDeliveryJob.class);

    private final ProductNotificationOutbox notificationOutbox;
    private final ProductNotificationEventRepository eventRepository;
    private final MeterRegistry meterRegistry;
    private final Counter deliveryRunsCounter;
    private final Counter deliveryAttemptedCounter;
    private final Counter deliverySentCounter;
    private final Counter deliveryFailedCounter;
    private final Timer deliveryTimer;

    public NotificationDeliveryJob(
            ProductNotificationOutbox notificationOutbox,
            ProductNotificationEventRepository eventRepository,
            MeterRegistry meterRegistry) {
        this.notificationOutbox = notificationOutbox;
        this.eventRepository = eventRepository;
        this.meterRegistry = meterRegistry;
        this.deliveryRunsCounter = Counter.builder("notifications.delivery.runs")
                .description("Number of notification outbox dispatcher runs")
                .register(meterRegistry);
        this.deliveryAttemptedCounter = Counter.builder("notifications.delivery.attempted")
                .description("Number of notification deliveries attempted")
                .register(meterRegistry);
        this.deliverySentCounter = Counter.builder("notifications.delivery.sent")
                .description("Number of notifications successfully delivered")
                .register(meterRegistry);
        this.deliveryFailedCounter = Counter.builder("notifications.delivery.failed")
                .description("Number of notification delivery attempts failed")
                .register(meterRegistry);
        this.deliveryTimer = Timer.builder("notifications.delivery.duration")
                .description("Execution duration of notification delivery cycle")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${monitor.notification.interval-ms:10000}")
    public synchronized void deliverPendingNotifications() {
        deliveryRunsCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ProductNotificationOutbox.DispatchResult result = notificationOutbox.dispatchDue();
            deliveryAttemptedCounter.increment(result.attempted());
            deliverySentCounter.increment(result.sent());
            deliveryFailedCounter.increment(result.failed());

            if (result.selected() > 0 || result.failed() > 0) {
                logger.atInfo()
                        .addKeyValue("selected", result.selected())
                        .addKeyValue("attempted", result.attempted())
                        .addKeyValue("sent", result.sent())
                        .addKeyValue("failed", result.failed())
                        .addKeyValue("skipped", result.skipped())
                        .addKeyValue("pending", eventRepository.countByStatus(NotificationDeliveryStatus.PENDING)
                                + eventRepository.countByStatus(NotificationDeliveryStatus.FAILED)
                                + eventRepository.countByStatus(NotificationDeliveryStatus.SENDING))
                        .addKeyValue("exhausted", eventRepository.countByStatus(NotificationDeliveryStatus.EXHAUSTED))
                        .log("Notification delivery cycle completed with activity");
            } else {
                logger.atDebug().log("Notification delivery cycle completed: no due events");
            }

            eventRepository.findFirstByStatusOrderByLastAttemptAtDesc(NotificationDeliveryStatus.FAILED)
                    .ifPresent(event -> logger.atWarn()
                            .addKeyValue("eventId", event.getId())
                            .addKeyValue("attempts", event.getAttemptCount())
                            .addKeyValue("error", event.getLastErrorSummary())
                            .log("Latest notification delivery failure"));
            eventRepository.findFirstByStatusOrderByLastAttemptAtDesc(NotificationDeliveryStatus.EXHAUSTED)
                    .ifPresent(event -> logger.atError()
                            .addKeyValue("eventId", event.getId())
                            .addKeyValue("attempts", event.getAttemptCount())
                            .addKeyValue("error", event.getLastErrorSummary())
                            .log("Notification delivery exhausted"));
        } catch (RuntimeException exception) {
            logger.error("Notification delivery cycle failed ({})", exception.getClass().getSimpleName());
        } finally {
            sample.stop(deliveryTimer);
        }
    }
}
