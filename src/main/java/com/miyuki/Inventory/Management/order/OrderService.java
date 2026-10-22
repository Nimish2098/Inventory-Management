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
import com.miyuki.Inventory.Management.stock.model.InventoryItem;
import com.miyuki.Inventory.Management.stock.repository.InventoryitemRepository;
import com.miyuki.Inventory.Management.stock.model.StockMovement;
import com.miyuki.Inventory.Management.stock.repository.StockMovementRepository;
import com.miyuki.Inventory.Management.stock.model.StockMovementType;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryitemRepository inventoryitemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUser currentUser;

    public OrderService(OrderRepository orderRepository,
                       CustomerService customerService,
                       ProductRepository productRepository,
                       WarehouseRepository warehouseRepository,
                       InventoryitemRepository inventoryitemRepository,
                       StockMovementRepository stockMovementRepository,
                       CurrentUser currentUser) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryitemRepository = inventoryitemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request == null || request.customer_id() == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Customer and at least one order item are required");
        }

        Map<InventoryKey, Long> requestedQuantities = new TreeMap<>(
                Comparator.comparing(InventoryKey::productId).thenComparing(InventoryKey::warehouseId));
        for (CreateOrderItemRequest itemRequest : request.items()) {
            validateItem(itemRequest);
            InventoryKey key = new InventoryKey(itemRequest.product_id(), itemRequest.warehouse_id());
            try {
                requestedQuantities.merge(key, itemRequest.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Combined item quantity is too large");
            }
        }

        Customer customer = customerService.findCustomer(request.customer_id());
        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);

        for (Map.Entry<InventoryKey, Long> requestedItem : requestedQuantities.entrySet()) {
            InventoryKey key = requestedItem.getKey();
            Product product = productRepository.findByIdAndOwner_Id(key.productId(), currentUser.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + key.productId() + " not found"));
            Warehouse warehouse = warehouseRepository.findByIdAndOwner_Id(key.warehouseId(), currentUser.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse " + key.warehouseId() + " not found"));

            InventoryItem inventoryItem = inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(
                    key.productId(), key.warehouseId(), currentUser.id()).orElseThrow(() ->
                    new InsufficientStockException("Inventory item for product " + key.productId() + " at warehouse " + key.warehouseId() + " not found"));

            long quantity = requestedItem.getValue();
            if (inventoryItem.getQuantity() < quantity) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getId() + " in warehouse " + warehouse.getId());
            }

            inventoryItem.setQuantity(inventoryItem.getQuantity() - quantity);
            inventoryitemRepository.save(inventoryItem);
            recordMovement(inventoryItem, -quantity, StockMovementType.OUTBOUND);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setWarehouse(warehouse);
            orderItem.setQuantity(quantity);
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return orderRepository.findAllByCustomer_Owner_Id(currentUser.id()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPendingOrders() {
        return orderRepository.findByStatusAndCustomer_Owner_Id(OrderStatus.PENDING, currentUser.id())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }

        List<OrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing((OrderItem item) -> item.getProduct().getId())
                        .thenComparing(item -> item.getWarehouse().getId()))
                .toList();
        for (OrderItem item : items) {
            InventoryItem inventoryItem = inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(
                    item.getProduct().getId(), item.getWarehouse().getId(), currentUser.id()).orElseThrow(() ->
                    new ResourceNotFoundException("Inventory item for product " + item.getProduct().getId()
                            + " at warehouse " + item.getWarehouse().getId() + " not found"));

            inventoryItem.setQuantity(Math.addExact(inventoryItem.getQuantity(), item.getQuantity()));
            inventoryitemRepository.save(inventoryItem);
            recordMovement(inventoryItem, item.getQuantity(), StockMovementType.INBOUND);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findByIdAndCustomer_Owner_Id(orderId, currentUser.id())
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

    private void recordMovement(InventoryItem item, long quantity, StockMovementType type) {
        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setQuantityChange(quantity);
        movement.setType(type);
        stockMovementRepository.save(movement);
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

    private record InventoryKey(Long productId, Long warehouseId) {
    }
}
