package com.miyuki.Inventory.Management.Controller;

import com.miyuki.Inventory.Management.Model.Customer;
import com.miyuki.Inventory.Management.Service.CustomerService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public Customer addCustomer(@RequestBody Customer customer) {
        return customerService.addCustomer(customer);
    }

    @GetMapping("/{customerId}")
    public Customer getCustomer(@PathVariable Long customerId) {
        return customerService.getCustomer(customerId);
    }

    @PostMapping("/buy-order")
    public String buyOrder(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Long quantity) {
        return customerService.buyOrder(customerId, productId, quantity);
    }

    @DeleteMapping("/cancel-order/{orderId}")
    public String cancelOrder(@PathVariable Long orderId) {
        return customerService.cancelOrder(orderId);
    }

    @GetMapping("/pending-order/{customerId}")
    public String pendingOrder(@PathVariable Long customerId) {
        return customerService.pendingOrder(customerId);
    }
}
