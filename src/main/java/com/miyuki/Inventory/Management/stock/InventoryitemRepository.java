package com.miyuki.Inventory.Management.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryitemRepository extends JpaRepository<InventoryItem,Long> {

    Optional<InventoryItem> findByProductIdAndWarehouseId(Long product_id,Long warehouse_id);
}
