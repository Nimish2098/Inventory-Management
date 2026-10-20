package com.miyuki.Inventory.Management.external;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_observation", indexes = {
        @Index(name = "idx_product_observation_watch_time", columnList = "watch_id,observed_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ProductObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watch_id", nullable = false)
    private ProductWatch watch;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal observedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockStatus stockStatus;

    @Column(precision = 19, scale = 4)
    private BigDecimal targetPriceAtObservation;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    public ProductObservation(
            ProductWatch watch,
            BigDecimal observedPrice,
            StockStatus stockStatus,
            BigDecimal targetPriceAtObservation,
            Instant observedAt) {
        this.watch = watch;
        this.observedPrice = observedPrice;
        this.stockStatus = stockStatus;
        this.targetPriceAtObservation = targetPriceAtObservation;
        this.observedAt = observedAt;
    }
}
