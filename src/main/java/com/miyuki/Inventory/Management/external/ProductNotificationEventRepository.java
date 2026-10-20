package com.miyuki.Inventory.Management.external;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface ProductNotificationEventRepository extends JpaRepository<ProductNotificationEvent, Long> {

    @Query("""
            select event.id from ProductNotificationEvent event
            where event.attemptCount < :maxAttempts
              and (event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.PENDING
                   or (event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.FAILED
                       and event.nextAttemptAt <= :now)
                   or (event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.SENDING
                       and event.nextAttemptAt <= :now))
            order by event.createdAt asc, event.id asc
            """)
    List<Long> findDueEventIds(
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            update ProductNotificationEvent event
            set event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.SENDING,
                event.attemptCount = event.attemptCount + 1,
                event.nextAttemptAt = :leaseExpiresAt,
                event.lastAttemptAt = :now,
                event.lastErrorSummary = null
            where event.id = :eventId
              and event.attemptCount < :maxAttempts
              and (event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.PENDING
                   or (event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.FAILED
                       and event.nextAttemptAt <= :now)
                   or (event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.SENDING
                       and event.nextAttemptAt <= :now))
            """)
    int claimIfDue(
            @Param("eventId") Long eventId,
            @Param("now") Instant now,
            @Param("leaseExpiresAt") Instant leaseExpiresAt,
            @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Transactional
    @Query("""
            update ProductNotificationEvent event
            set event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.EXHAUSTED,
                event.lastErrorSummary = 'Delivery lease expired after maximum attempts'
            where event.status = com.miyuki.Inventory.Management.external.NotificationDeliveryStatus.SENDING
              and event.nextAttemptAt <= :now
              and event.attemptCount >= :maxAttempts
            """)
    int exhaustExpiredClaims(@Param("now") Instant now, @Param("maxAttempts") int maxAttempts);

    long countByStatus(NotificationDeliveryStatus status);

    java.util.Optional<ProductNotificationEvent> findFirstByStatusOrderByLastAttemptAtDesc(
            NotificationDeliveryStatus status);

    List<ProductNotificationEvent> findAllByWatch_Id(Long watchId);

    List<ProductNotificationEvent> findAllByInventoryItemId(Long inventoryItemId);

    void deleteAllByWatch_Id(Long watchId);

    void deleteAllByInventoryItemId(Long inventoryItemId);
}
