package com.miyuki.Inventory.Management.customer;

import com.miyuki.Inventory.Management.common.exception.ResourceNotFoundException;
import com.miyuki.Inventory.Management.customer.dto.CreateCustomerRequest;
import com.miyuki.Inventory.Management.customer.dto.CustomerResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (request == null || request.name() == null || request.name().isBlank() || request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Customer name and email are required");
        }

        if (customerRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Customer email already exists");
        }

        Customer customer = Customer.builder()
                .name(request.name())
                .email(request.email())
                .build();

        return toResponse(customerRepository.save(customer));
    }

    public List<CustomerResponse> getCustomers() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CustomerResponse getCustomer(Long customerId) {
        return toResponse(findCustomer(customerId));
    }

    public Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + customerId + " not found"));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail());
    }
}
