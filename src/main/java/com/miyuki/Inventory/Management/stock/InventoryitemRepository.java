package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InventoryitemRepository extends JpaRepository<InventoryItem,Long> {

    Optional<InventoryItem> findByProductIdAndWarehouseId(Long product_id,Long warehouse_id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findByProductIdAndWarehouseIdForUpdate(Long product_id,Long warehouse_id);

    List<InventoryItem> findByQuantityLessThanReorderLevel();
    List<InventoryItem> findByWarehouseId(Long warehouse_id);
 }
