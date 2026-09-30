package com.miyuki.Inventory.Management.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement,Long> {

    @Query("select movement from StockMovement movement where movement.inventoryItem.product.id = :productId " +
	    "and movement.inventoryItem.warehouse.id = :warehouseId order by movement.id desc")
    List<StockMovement> findByProductAndWarehouse(@Param("productId") Long productId,
						   @Param("warehouseId") Long warehouseId);
}
