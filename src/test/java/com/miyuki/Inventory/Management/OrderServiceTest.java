package com.miyuki.Inventory.Management;

import com.miyuki.Inventory.Management.customer.entity.Customer;
import com.miyuki.Inventory.Management.order.entity.Order;
import com.miyuki.Inventory.Management.product.Product;
import com.miyuki.Inventory.Management.customer.repository.CustomerRepository;
import com.miyuki.Inventory.Management.order.repository.OrderRepository;
import com.miyuki.Inventory.Management.product.ProductRepository;
import com.miyuki.Inventory.Management.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldCreateConfirmedOrderAndDecreaseStock() {
        Customer customer = new Customer();
        customer.setCustomer_email("alice@example.com");
        customer.setCustomer_address("Main Street");
        customer = customerRepository.save(customer);

        Product product = new Product();
        product.setProduct_name("Mouse");
        product.setProduct_stock(10L);
        product = productRepository.save(product);

        Order order = orderService.buyOrder(customer.getCustomer_id(), product.getProduct_id(), 3);

        assertEquals("CONFIRMED", order.getOrder_status());
        assertEquals(7L, productRepository.findById(product.getProduct_id()).orElseThrow().getProduct_stock());
        assertEquals(1, orderRepository.count());
    }

    @Test
    void shouldRollbackWhenInventoryIsInsufficient() {
        Customer customer = new Customer();
        customer.setCustomer_email("bob@example.com");
        customer.setCustomer_address("Second Street");
        customer = customerRepository.save(customer);

        Product product = new Product();
        product.setProduct_name("Keyboard");
        product.setProduct_stock(2L);
        product = productRepository.save(product);

        try {
            orderService.buyOrder(customer.getCustomer_id(), product.getProduct_id(), 5);
        } catch (IllegalArgumentException ignored) {
            // expected rollback path
        }

        assertEquals(2L, productRepository.findById(product.getProduct_id()).orElseThrow().getProduct_stock());
        assertEquals(1, orderRepository.count());
    }

    @Test
    void shouldCancelOrderAndRestorePurchasedQuantity() {
        Customer customer = new Customer();
        customer.setCustomer_email("cancel@example.com");
        customer.setCustomer_address("Cancel Street");
        customer = customerRepository.save(customer);

        Product product = new Product();
        product.setProduct_name("Monitor");
        product.setProduct_stock(10L);
        product = productRepository.save(product);

        Order order = orderService.buyOrder(customer.getCustomer_id(), product.getProduct_id(), 3);
        Order cancelled = orderService.cancelOrder(order.getOrder_id());

        assertEquals("CANCELLED", cancelled.getOrder_status());
        assertEquals(10L, productRepository.findById(product.getProduct_id()).orElseThrow().getProduct_stock());
    }
}
