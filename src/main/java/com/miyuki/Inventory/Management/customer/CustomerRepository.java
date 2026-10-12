package com.miyuki.Inventory.Management.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmailAndOwner_Id(String email, Long ownerId);
    Optional<Customer> findByIdAndOwner_Id(Long id, Long ownerId);
    java.util.List<Customer> findAllByOwner_Id(Long ownerId);
}
