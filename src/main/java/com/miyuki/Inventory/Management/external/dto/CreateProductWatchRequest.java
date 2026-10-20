package com.miyuki.Inventory.Management.external.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductWatchRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Product name must be at most 200 characters")
        String productName,

        @NotBlank(message = "Product URL is required")
        @Size(max = 2048, message = "Product URL must be at most 2048 characters")
        String productUrl,

        @Positive(message = "Target price must be greater than zero")
        BigDecimal targetPrice) {
}
