package com.miyuki.Inventory.Management.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryAlertStateRepository extends JpaRepository<InventoryAlertState, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from InventoryAlertState state where state.inventoryItemId = :inventoryItemId")
    Optional<InventoryAlertState> findByInventoryItemIdForUpdate(
            @Param("inventoryItemId") Long inventoryItemId);

    void deleteByInventoryItemId(Long inventoryItemId);
}
