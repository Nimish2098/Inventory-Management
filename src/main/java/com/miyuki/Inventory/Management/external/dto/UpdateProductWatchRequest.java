package com.miyuki.Inventory.Management.external.dto;

import java.math.BigDecimal;

public record UpdateProductWatchRequest(
        BigDecimal targetPrice,
        Boolean enabled,
        boolean clearTargetPrice) {
}
