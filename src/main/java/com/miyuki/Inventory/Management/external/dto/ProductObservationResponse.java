package com.miyuki.Inventory.Management.external.dto;

import com.miyuki.Inventory.Management.external.StockStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductObservationResponse(
        Long id,
        BigDecimal observedPrice,
        StockStatus stockStatus,
        Instant observedAt) {
}
