package com.miyuki.Inventory.Management.external.dto;

import com.miyuki.Inventory.Management.external.StockStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductWatchResponse(
        Long id,
        String productName,
        String productUrl,
        BigDecimal targetPrice,
        BigDecimal latestObservedPrice,
        StockStatus latestStockStatus,
        Instant lastSuccessfulCheck,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {
}
