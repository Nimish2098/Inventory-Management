package com.miyuki.Inventory.Management.order;

import com.miyuki.Inventory.Management.common.exception.InsufficientStockException;
import com.miyuki.Inventory.Management.customer.Customer;
import com.miyuki.Inventory.Management.customer.CustomerService;
import com.miyuki.Inventory.Management.order.dto.CreateOrderItemRequest;
import com.miyuki.Inventory.Management.order.dto.CreateOrderRequest;
import com.miyuki.Inventory.Management.order.dto.OrderResponse;
import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.model.InventoryItem;
import com.miyuki.Inventory.Management.stock.repository.InventoryitemRepository;
import com.miyuki.Inventory.Management.stock.model.StockMovement;
import com.miyuki.Inventory.Management.stock.repository.StockMovementRepository;
import com.miyuki.Inventory.Management.stock.model.StockMovementType;
import com.miyuki.Inventory.Management.user.CurrentUser;
import com.miyuki.Inventory.Management.user.UserAccount;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CustomerService customerService = mock(CustomerService.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    private final InventoryitemRepository inventoryitemRepository = mock(InventoryitemRepository.class);
    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final CurrentUser currentUser = new CurrentUser();

    private final OrderService orderService = new OrderService(
            orderRepository,
            customerService,
            productRepository,
            warehouseRepository,
            inventoryitemRepository,
            stockMovementRepository,
            currentUser);

    private Customer customer;
    private Product product;
    private Warehouse warehouse;
    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        UserAccount user = new UserAccount();
        user.setId(5L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        customer = new Customer();
        customer.setId(7L);
        product = new Product();
        product.setId(11L);
        warehouse = new Warehouse();
        warehouse.setId(13L);
        inventoryItem = new InventoryItem();
        inventoryItem.setProduct(product);
        inventoryItem.setWarehouse(warehouse);
        inventoryItem.setQuantity(5L);

        when(customerService.findCustomer(7L)).thenReturn(customer);
        when(productRepository.findByIdAndOwner_Id(11L, 5L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByIdAndOwner_Id(13L, 5L)).thenReturn(Optional.of(warehouse));
        when(inventoryitemRepository.findByProductIdAndWarehouseIdForUpdate(11L, 13L, 5L))
                .thenReturn(Optional.of(inventoryItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrderReservesStockAndLeavesOrderPending() {
        OrderResponse response = orderService.createOrder(orderRequest(3L));

        assertEquals("PENDING", response.status());
        assertEquals(2L, inventoryItem.getQuantity());
        assertEquals(1, response.items().size());
        assertEquals(3L, response.items().getFirst().quantity());

        verify(inventoryitemRepository).save(inventoryItem);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void createOrderRejectsInsufficientStockWithoutPersistingOrderOrMovement() {
        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(orderRequest(6L)));

        assertEquals("Insufficient stock for product 11 in warehouse 13", exception.getMessage());
        assertEquals(5L, inventoryItem.getQuantity());
        verify(orderRepository, never()).save(any(Order.class));
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void duplicateItemsAreAggregatedBeforeReservingStock() {
        CreateOrderRequest request = new CreateOrderRequest(7L, List.of(
                new CreateOrderItemRequest(11L, 13L, 2L),
                new CreateOrderItemRequest(11L, 13L, 1L)));

        OrderResponse response = orderService.createOrder(request);

        assertEquals(2L, inventoryItem.getQuantity());
        assertEquals(1, response.items().size());
        assertEquals(3L, response.items().getFirst().quantity());
        verify(inventoryitemRepository).findByProductIdAndWarehouseIdForUpdate(11L, 13L, 5L);
    }

    @Test
    void cancelOrderRestoresStockAndIsIdempotent() {
        Order order = new Order();
        order.setId(17L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setWarehouse(warehouse);
        orderItem.setQuantity(3L);
        order.addItem(orderItem);
        when(orderRepository.findByIdForUpdate(17L, 5L)).thenReturn(Optional.of(order));
        inventoryItem.setQuantity(2L);

        OrderResponse response = orderService.cancelOrder(17L);
        orderService.cancelOrder(17L);

        assertEquals("CANCELLED", response.status());
        assertEquals(5L, inventoryItem.getQuantity());
        org.mockito.ArgumentCaptor<StockMovement> movementCaptor =
                org.mockito.ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertEquals(3L, movementCaptor.getValue().getQuantityChange());
        assertEquals(StockMovementType.INBOUND, movementCaptor.getValue().getType());
        verify(inventoryitemRepository).save(inventoryItem);
    }

    @Test
    void createOrderRecordsOutboundMovement() {
        orderService.createOrder(orderRequest(2L));

        org.mockito.ArgumentCaptor<StockMovement> movementCaptor =
                org.mockito.ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertEquals(-2L, movementCaptor.getValue().getQuantityChange());
        assertEquals(StockMovementType.OUTBOUND, movementCaptor.getValue().getType());
    }

    private CreateOrderRequest orderRequest(Long quantity) {
        return new CreateOrderRequest(7L, List.of(new CreateOrderItemRequest(11L, 13L, quantity)));
    }
}
