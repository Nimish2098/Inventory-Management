package com.miyuki.Inventory.Management.order.dto;

import java.util.List;

public record CreateOrderRequest(
        Long customer_id,
        List<CreateOrderItemRequest> items
) {
}
