package com.miyuki.Inventory.Management.Repository;

import com.miyuki.Inventory.Management.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order,Long> {
}
