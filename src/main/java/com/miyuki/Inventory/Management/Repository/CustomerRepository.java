package com.miyuki.Inventory.Management.Repository;

import com.miyuki.Inventory.Management.Model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
