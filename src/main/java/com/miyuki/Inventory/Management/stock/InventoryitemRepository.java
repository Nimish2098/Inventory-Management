package com.miyuki.Inventory.Management.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryitemRepository extends JpaRepository<InventoryItem,Long> {

    @Query("select item from InventoryItem item where item.product.id = :productId and item.warehouse.id = :warehouseId")
    Optional<InventoryItem> findByProductIdAndWarehouseId(@Param("productId") Long productId,
                                                          @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("select item from InventoryItem item where item.product.id = :productId and item.warehouse.id = :warehouseId " +
             "and item.product.owner.id = :ownerId and item.warehouse.owner.id = :ownerId")
     Optional<InventoryItem> findByProductIdAndWarehouseIdForUpdate(@Param("productId") Long productId,
                                                                    @Param("warehouseId") Long warehouseId,
                                                                    @Param("ownerId") Long ownerId);

     @Query("select item from InventoryItem item where item.quantity < item.product.reorderLevel " +
             "and item.product.owner.id = :ownerId and item.warehouse.owner.id = :ownerId")
     List<InventoryItem> findLowStockItems(@Param("ownerId") Long ownerId);

     @Query("select item from InventoryItem item where item.product.owner.id = :ownerId " +
             "and item.warehouse.owner.id = :ownerId")
     List<InventoryItem> findAllByOwnerId(@Param("ownerId") Long ownerId);

     @EntityGraph(attributePaths = {"product", "warehouse"})
     @Query("select item from InventoryItem item " +
             "where item.product.owner.id = item.warehouse.owner.id")
     List<InventoryItem> findAllForMonitoring();

     List<InventoryItem> findByWarehouse_IdAndProduct_Owner_IdAndWarehouse_Owner_Id(
             Long warehouseId, Long productOwnerId, Long warehouseOwnerId);

     Optional<InventoryItem> findByProduct_IdAndWarehouse_IdAndProduct_Owner_IdAndWarehouse_Owner_Id(
             Long productId, Long warehouseId, Long productOwnerId, Long warehouseOwnerId);
 }
