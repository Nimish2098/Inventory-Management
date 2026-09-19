package com.miyuki.Inventory.Management.Service;

import com.miyuki.Inventory.Management.Model.Customer;
import com.miyuki.Inventory.Management.Repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService{


    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    public Customer addCustomer(Customer customer){
        return customerRepository.save(customer);
    }

    public Customer getCustomer(Long customer_id){
        return customerRepository.findById(customer_id).orElse(null);
    }

    public void getCustomerBuyOrder(){

    }
}