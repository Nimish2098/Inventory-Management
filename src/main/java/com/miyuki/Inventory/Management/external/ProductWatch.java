package com.miyuki.Inventory.Management.external;

import com.miyuki.Inventory.Management.user.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_watch", indexes = {
        @Index(name = "idx_product_watch_owner", columnList = "owner_id"),
        @Index(name = "idx_product_watch_enabled_id", columnList = "enabled,id")
})
@Getter
@Setter
@NoArgsConstructor
public class ProductWatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 2048)
    private String productUrl;

    @Column(precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal latestObservedPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StockStatus latestStockStatus;

    private Instant lastSuccessfulCheck;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
