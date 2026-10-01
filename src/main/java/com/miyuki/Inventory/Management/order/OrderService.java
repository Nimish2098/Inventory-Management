package com.miyuki.Inventory.Management.order;

import com.miyuki.Inventory.Management.common.exception.InsufficientStockException;
import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.customer.Customer;
import com.miyuki.Inventory.Management.customer.CustomerService;
import com.miyuki.Inventory.Management.order.dto.CreateOrderItemRequest;
import com.miyuki.Inventory.Management.order.dto.CreateOrderRequest;
import com.miyuki.Inventory.Management.order.dto.OrderItemResponse;
import com.miyuki.Inventory.Management.order.dto.OrderResponse;
import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.InventoryItem;
import com.miyuki.Inventory.Management.stock.InventoryitemRepository;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryitemRepository inventoryitemRepository;

    public OrderService(OrderRepository orderRepository,
                       CustomerService customerService,
                       ProductRepository productRepository,
                       WarehouseRepository warehouseRepository,
                       InventoryitemRepository inventoryitemRepository) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryitemRepository = inventoryitemRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request == null || request.customer_id() == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Customer and at least one order item are required");
        }

        Customer customer = customerService.findCustomer(request.customer_id());
        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);

        for (CreateOrderItemRequest itemRequest : request.items()) {
            validateItem(itemRequest);

            Product product = productRepository.findById(itemRequest.product_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + itemRequest.product_id() + " not found"));
            Warehouse warehouse = warehouseRepository.findById(itemRequest.warehouse_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse " + itemRequest.warehouse_id() + " not found"));

            InventoryItem inventoryItem = inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(
                    itemRequest.product_id(), itemRequest.warehouse_id()).orElseThrow(() ->
                    new InsufficientStockException("Inventory item for product " + itemRequest.product_id() + " at warehouse " + itemRequest.warehouse_id() + " not found"));

            if (inventoryItem.getQuantity() < itemRequest.quantity()) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getId() + " in warehouse " + warehouse.getId());
            }

            inventoryItem.setQuantity(inventoryItem.getQuantity() - itemRequest.quantity());
            inventoryitemRepository.save(inventoryItem);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setWarehouse(warehouse);
            orderItem.setQuantity(itemRequest.quantity());
            order.addItem(orderItem);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING).stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toResponse(order);
        }

        for (OrderItem item : order.getItems()) {
            InventoryItem inventoryItem = inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(
                    item.getProduct().getId(), item.getWarehouse().getId()).orElseGet(() -> {
                        InventoryItem newInventoryItem = new InventoryItem();
                        newInventoryItem.setProduct(item.getProduct());
                        newInventoryItem.setWarehouse(item.getWarehouse());
                        newInventoryItem.setQuantity(0L);
                        return newInventoryItem;
                    });

            inventoryItem.setQuantity(inventoryItem.getQuantity() + item.getQuantity());
            inventoryitemRepository.save(inventoryItem);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
    }

    private void validateItem(CreateOrderItemRequest itemRequest) {
        if (itemRequest == null) {
            throw new IllegalArgumentException("Order item is required");
        }
        if (itemRequest.product_id() == null || itemRequest.warehouse_id() == null || itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
            throw new IllegalArgumentException("Product, warehouse, and a positive quantity are required");
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            items.add(new OrderItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getWarehouse().getId(),
                    item.getQuantity()
            ));
        }

        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items
        );
    }
}
