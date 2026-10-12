package com.miyuki.Inventory.Management.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer_Id(Long customerId);
    List<Order> findAllByCustomer_Owner_Id(Long ownerId);
    List<Order> findByStatusAndCustomer_Owner_Id(OrderStatus status, Long ownerId);
    Optional<Order> findByIdAndCustomer_Owner_Id(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId and o.customer.owner.id = :ownerId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId, @Param("ownerId") Long ownerId);
}
