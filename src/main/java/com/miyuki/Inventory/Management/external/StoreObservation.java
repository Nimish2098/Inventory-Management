package com.miyuki.Inventory.Management.external;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record StoreObservation(BigDecimal price, StockStatus stockStatus, Instant observedAt) {

    public StoreObservation {
        Objects.requireNonNull(price, "Observed price is required");
        Objects.requireNonNull(stockStatus, "Observed stock status is required");
        Objects.requireNonNull(observedAt, "Observation timestamp is required");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Observed price cannot be negative");
        }
    }
}
