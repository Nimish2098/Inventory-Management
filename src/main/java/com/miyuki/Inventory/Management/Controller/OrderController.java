package com.miyuki.Inventory.Management.Controller;

import com.miyuki.Inventory.Management.Model.Order;
import com.miyuki.Inventory.Management.Service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderCreateRequest request) {
        Order order = orderService.createOrder(request.getCustomerId(), request.getItems());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    public static class OrderCreateRequest {
        private Long customerId;
        private List<OrderService.OrderItemRequest> items;

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public List<OrderService.OrderItemRequest> getItems() {
            return items;
        }

        public void setItems(List<OrderService.OrderItemRequest> items) {
            this.items = items;
        }
    }
}

