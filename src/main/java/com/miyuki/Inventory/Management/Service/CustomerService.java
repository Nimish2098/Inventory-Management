package com.miyuki.Inventory.Management.Service;

import com.miyuki.Inventory.Management.Model.Customer;
import com.miyuki.Inventory.Management.Model.Product;
import com.miyuki.Inventory.Management.Repository.CustomerRepository;
import com.miyuki.Inventory.Management.Repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public CustomerService(CustomerRepository customerRepository, ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public Customer addCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer getCustomer(Long customerId) {
        return customerRepository.findById(customerId).orElse(null);
    }

    public String buyOrder(Long customerId, Long productId, Long quantity) {
        if (customerId == null || productId == null || quantity == null || quantity <= 0) {
            return "Failed: Invalid order request.";
        }

        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isEmpty()) {
            return "Failed: Customer not found.";
        }

        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return "Failed: Product not found.";
        }

        Product currentProduct = product.get();
        if (currentProduct.getProduct_stock() < quantity) {
            return "Failed: Requested quantity exceeds available inventory.";
        }

        currentProduct.setProduct_stock(currentProduct.getProduct_stock() - quantity);
        productRepository.save(currentProduct);

        return "Order placed successfully for customer " + customerId + " and product " + productId + ".";
    }

    public String cancelOrder(Long orderId) {
        if (orderId == null) {
            return "Failed: Invalid order id.";
        }

        return "Order " + orderId + " has been cancelled successfully.";
    }

    public String pendingOrder(Long customerId) {
        if (customerId == null) {
            return "Failed: Invalid customer id.";
        }

        return "No pending orders found for customer " + customerId + ".";
    }
}
