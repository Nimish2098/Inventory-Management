package com.miyuki.Inventory.Management.order.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customer_id,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}
