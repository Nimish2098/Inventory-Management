package com.miyuki.Inventory.Management.order;

import com.miyuki.Inventory.Management.common.exception.InsufficientStockException;
import com.miyuki.Inventory.Management.customer.Customer;
import com.miyuki.Inventory.Management.customer.CustomerRepository;
import com.miyuki.Inventory.Management.order.dto.CreateOrderItemRequest;
import com.miyuki.Inventory.Management.order.dto.CreateOrderRequest;
import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.stock.InventoryItem;
import com.miyuki.Inventory.Management.stock.InventoryitemRepository;
import com.miyuki.Inventory.Management.stock.StockMovementRepository;
import com.miyuki.Inventory.Management.user.UserAccount;
import com.miyuki.Inventory.Management.user.UserAccountRepository;
import com.miyuki.Inventory.Management.warehouses.Warehouse;
import com.miyuki.Inventory.Management.warehouses.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryitemRepository inventoryitemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private UserAccountRepository userRepository;

    private UserAccount user;
    private Long customerId;
    private Long productId;
    private Long warehouseId;

    @BeforeEach
    void setUp() {
        String uniqueId = UUID.randomUUID().toString();

        user = new UserAccount();
        user.setName("Concurrency test user");
        user.setEmail("user-" + uniqueId + "@example.test");
        user.setPassword("unused-test-hash");
        user = userRepository.saveAndFlush(user);

        Customer customer = new Customer();
        customer.setName("Concurrency test");
        customer.setEmail("concurrency-" + uniqueId + "@example.test");
        customer.setOwner(user);
        customerId = customerRepository.saveAndFlush(customer).getId();

        Product product = new Product();
        product.setSku("CONCURRENCY-" + uniqueId);
        product.setName("Concurrency test product");
        product.setOwner(user);
        productId = productRepository.saveAndFlush(product).getId();

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("CONCURRENCY-" + uniqueId);
        warehouse.setName("Concurrency test warehouse");
        warehouse.setActive(true);
        warehouse.setOwner(user);
        warehouseId = warehouseRepository.saveAndFlush(warehouse).getId();

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProduct(product);
        inventoryItem.setWarehouse(warehouse);
        inventoryItem.setQuantity(10L);
        inventoryitemRepository.saveAndFlush(inventoryItem);
    }

    @AfterEach
    void cleanUp() {
        if (customerId != null) {
            orderRepository.deleteAll(orderRepository.findByCustomer_Id(customerId));
        }
        if (productId != null && warehouseId != null) {
            stockMovementRepository.deleteAll(
                    stockMovementRepository.findByProductAndWarehouse(productId, warehouseId));
            inventoryitemRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                    .ifPresent(inventoryitemRepository::delete);
        }
        if (customerId != null) {
            customerRepository.deleteById(customerId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (warehouseId != null) {
            warehouseRepository.deleteById(warehouseId);
        }
        if (user != null) {
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    void concurrentOrdersNeverAllocateMoreThanAvailableStock() throws Exception {
        int requestCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Boolean>> requests = new ArrayList<>(requestCount);
        try {
            for (int index = 0; index < requestCount; index++) {
                requests.add(executor.submit(() -> {
                    startGate.await();
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                            user, null, user.getAuthorities()));
                    SecurityContextHolder.setContext(context);
                    try {
                        orderService.createOrder(new CreateOrderRequest(
                                customerId,
                                List.of(new CreateOrderItemRequest(productId, warehouseId, 1L))));
                        return true;
                    } catch (InsufficientStockException exception) {
                        return false;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            startGate.countDown();
            int accepted = 0;
            for (Future<Boolean> request : requests) {
                if (request.get()) {
                    accepted++;
                }
            }

            assertEquals(10, accepted);
            assertEquals(0L, inventoryitemRepository
                    .findByProductIdAndWarehouseId(productId, warehouseId).orElseThrow().getQuantity());
            assertEquals(10, orderRepository.findByCustomer_Id(customerId).size());
            assertEquals(10, stockMovementRepository
                    .findByProductAndWarehouse(productId, warehouseId).size());
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent order requests did not terminate");
            }
        }
    }
}
