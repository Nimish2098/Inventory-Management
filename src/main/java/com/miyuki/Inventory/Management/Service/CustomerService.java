package com.miyuki.Inventory.Management.Service;

import com.miyuki.Inventory.Management.Model.Customer;
import com.miyuki.Inventory.Management.Repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer addCustomer(Customer customer) {
        return null;
    }

    public Customer getCustomer(Long customerId) {
        return null;
    }

    public String buyOrder(Long customerId, Long productId, Long quantity) {
        return null;
    }

    public String cancelOrder(Long orderId) {
        return null;
    }

    public String pendingOrder(Long customerId) {
        return null;
    }
}
