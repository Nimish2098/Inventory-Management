package com.miyuki.Inventory.Management;

import com.miyuki.Inventory.Management.Model.Customer;
import com.miyuki.Inventory.Management.Model.Product;
import com.miyuki.Inventory.Management.Repository.CustomerRepository;
import com.miyuki.Inventory.Management.Repository.ProductRepository;
import com.miyuki.Inventory.Management.Service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customerService = new CustomerService(customerRepository, productRepository);
    }

    @Test
    void addCustomer_shouldSaveAndReturnCustomer() {
        Customer customer = new Customer();
        customer.setCustomerName("Alice");

        when(customerRepository.save(customer)).thenReturn(customer);

        assertSame(customer, customerService.addCustomer(customer));
    }

    @Test
    void buyOrder_shouldRejectWhenStockIsInsufficient() {
        Customer customer = new Customer(1L, "Alice", "alice@example.com");
        Product product = new Product(10L, "Laptop", 2L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        String result = customerService.buyOrder(1L, 10L, 5L);

        assertTrue(result.contains("Failed"));
    }
}
