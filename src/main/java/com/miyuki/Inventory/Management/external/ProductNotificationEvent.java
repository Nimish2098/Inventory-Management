package com.miyuki.Inventory.Management.external;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "product_notification_event", indexes = {
        @Index(name = "idx_product_notification_pending", columnList = "status,next_attempt_at,created_at"),
        @Index(name = "idx_product_notification_watch", columnList = "watch_id,created_at"),
        @Index(name = "idx_product_notification_inventory", columnList = "inventory_item_id,created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ProductNotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watch_id")
    private ProductWatch watch;

    @Column(name = "inventory_item_id")
    private Long inventoryItemId;

    @Column(name = "event_key", unique = true, length = 300)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductEventType eventType;

    @Column(precision = 19, scale = 4)
    private BigDecimal previousPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 100)
    private String warehouseCode;

    private Long currentQuantity;

    private Long reorderLevel;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(length = 500)
    private String lastErrorSummary;

    private Instant deliveredAt;

    public ProductNotificationEvent(
            ProductWatch watch,
            ProductEventType eventType,
            BigDecimal previousPrice,
            BigDecimal currentPrice,
            BigDecimal targetPrice,
            Instant createdAt,
            String eventKey) {
        this.watch = watch;
        this.eventKey = Objects.requireNonNull(eventKey);
        this.eventType = eventType;
        this.previousPrice = previousPrice;
        this.currentPrice = currentPrice;
        this.targetPrice = targetPrice;
        this.productName = watch.getProductName();
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public boolean isExternal() {
        return watch != null;
    }

    @PrePersist
    void initializeDeliveryState() {
        if (status == null) {
            status = NotificationDeliveryStatus.PENDING;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt == null ? Instant.now() : createdAt;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public ProductNotificationEvent(
            com.miyuki.Inventory.Management.stock.InventoryItem inventoryItem,
            long episode,
            Instant createdAt) {
        this.inventoryItemId = inventoryItem.getId();
        this.eventKey = "inventory:" + inventoryItem.getId() + ":low-stock:" + episode;
        this.eventType = ProductEventType.INTERNAL_LOW_STOCK;
        this.currentPrice = BigDecimal.ZERO;
        this.productName = inventoryItem.getProduct().getName();
        this.warehouseCode = inventoryItem.getWarehouse().getCode();
        this.currentQuantity = inventoryItem.getQuantity();
        this.reorderLevel = inventoryItem.getProduct().getReorderLevel();
        this.createdAt = createdAt;
        this.nextAttemptAt = createdAt;
    }
}
