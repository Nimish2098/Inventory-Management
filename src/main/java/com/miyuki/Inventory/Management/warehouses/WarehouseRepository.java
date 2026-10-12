package com.miyuki.Inventory.Management.warehouses;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse,Long> {

    Optional<Warehouse> findByIdAndOwner_Id(Long id, Long ownerId);
    Optional<Warehouse> findByCodeAndOwner_Id(String code, Long ownerId);
    boolean existsByCodeAndOwner_Id(String code, Long ownerId);
    List<Warehouse> findByActiveTrueAndOwner_Id(Long ownerId);
}
