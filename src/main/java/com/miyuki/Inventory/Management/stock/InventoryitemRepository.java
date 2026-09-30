package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryitemRepository extends JpaRepository<InventoryItem,Long> {

    @Query("select item from InventoryItem item where item.product.id = :productId and item.warehouse.id = :warehouseId")
    Optional<InventoryItem> findByProductIdAndWarehouseId(@Param("productId") Long productId,
                                                          @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item where item.product.id = :productId and item.warehouse.id = :warehouseId")
    Optional<InventoryItem> findByProductIdAndWarehouseIdForUpdate(@Param("productId") Long productId,
                                                                   @Param("warehouseId") Long warehouseId);

    @Query("select item from InventoryItem item where item.quantity < item.product.reorderLevel")
    List<InventoryItem> findLowStockItems();

    List<InventoryItem> findByWarehouse_Id(Long warehouseId);
 }
